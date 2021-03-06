/*******************************************************************************
 *
 *    Copyright (C) 2015-2017 the BBoxDB project
 *  
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License. 
 *    
 *******************************************************************************/
package org.bboxdb.network.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.bboxdb.distribution.DistributionGroupCache;
import org.bboxdb.distribution.DistributionRegion;
import org.bboxdb.distribution.membership.DistributedInstance;
import org.bboxdb.distribution.membership.MembershipConnectionService;
import org.bboxdb.distribution.mode.KDtreeZookeeperAdapter;
import org.bboxdb.distribution.placement.RandomResourcePlacementStrategy;
import org.bboxdb.distribution.placement.ResourceAllocationException;
import org.bboxdb.distribution.placement.ResourcePlacementStrategy;
import org.bboxdb.distribution.zookeeper.ZookeeperClient;
import org.bboxdb.distribution.zookeeper.ZookeeperException;
import org.bboxdb.network.NetworkConnectionState;
import org.bboxdb.network.client.future.EmptyResultFuture;
import org.bboxdb.network.client.future.FutureHelper;
import org.bboxdb.network.client.future.SSTableNameListFuture;
import org.bboxdb.network.client.future.TupleListFuture;
import org.bboxdb.network.routing.RoutingHeader;
import org.bboxdb.network.routing.RoutingHop;
import org.bboxdb.network.routing.RoutingHopHelper;
import org.bboxdb.storage.entity.BoundingBox;
import org.bboxdb.storage.entity.SSTableName;
import org.bboxdb.storage.entity.Tuple;
import org.bboxdb.util.MicroSecondTimestampProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BBoxDBCluster implements BBoxDB {
	
	/**
	 * The Zookeeper connection
	 */
	protected final ZookeeperClient zookeeperClient;
	
	/**
	 * The number of in flight requests
	 * @return
	 */
	protected volatile short maxInFlightCalls = MAX_IN_FLIGHT_CALLS;
	
	/**
	 * The resource placement strategy
	 */
	protected final ResourcePlacementStrategy resourcePlacementStrategy;
	
	/**
	 * The membership connection service
	 */
	protected final MembershipConnectionService membershipConnectionService;
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(BBoxDBCluster.class);

	/**
	 * Create a new instance of the BBoxDB cluster
	 * @param zookeeperNodes
	 * @param clustername
	 */
	public BBoxDBCluster(final Collection<String> zookeeperNodes, final String clustername) {
		zookeeperClient = new ZookeeperClient(zookeeperNodes, clustername);
		resourcePlacementStrategy = new RandomResourcePlacementStrategy();
		membershipConnectionService = MembershipConnectionService.getInstance();
	}
	
	/**
	 * Create a new instance of the BBoxDB cluster
	 * @param zookeeperNodes
	 * @param clustername
	 */
	public BBoxDBCluster(final String zookeeperNode, final String clustername) {
		this(Arrays.asList(zookeeperNode), clustername);
	}
	

	@Override
	public boolean connect() {
		zookeeperClient.init();
		zookeeperClient.startMembershipObserver();
		membershipConnectionService.init();
		return zookeeperClient.isConnected();
	}

	@Override
	public void disconnect() {
		membershipConnectionService.shutdown();
		zookeeperClient.shutdown();		
	}

	@Override
	public EmptyResultFuture deleteTable(final String table) throws BBoxDBException {
		
		if(membershipConnectionService.getNumberOfConnections() == 0) {
			throw new BBoxDBException("deleteTable called, but connection list is empty");
		}
		
		final List<BBoxDBClient> connections = membershipConnectionService.getAllConnections();
		final EmptyResultFuture future = new EmptyResultFuture();
		
		connections.stream()
		 	.map(c -> c.deleteTable(table))
		 	.filter(Objects::nonNull)
		 	.forEach(f -> future.merge(f));
		
		return future;
	}

	@Override
	public EmptyResultFuture insertTuple(final String table, final Tuple tuple) throws BBoxDBException {

		try {
			final SSTableName ssTableName = new SSTableName(table);
			
			final KDtreeZookeeperAdapter distributionAdapter = DistributionGroupCache.getGroupForTableName(
					ssTableName, zookeeperClient);

			final DistributionRegion distributionRegion = distributionAdapter.getRootNode();
			
			final List<RoutingHop> hops = RoutingHopHelper.getRoutingHopsForWrite(tuple, distributionRegion);
			
			if(hops.isEmpty()) {
				logger.error("Insert tuple called, but hop list for bounding box is empty: {}", 
						tuple.getBoundingBox());
				return FutureHelper.getFailedEmptyResultFuture();
			}
			
			// Determine the first system, it will route the request to the remaining systems
			final DistributedInstance system = hops.iterator().next().getDistributedInstance();
			final BBoxDBClient connection = membershipConnectionService.getConnectionForInstance(system);
			
			if(connection == null) {
				logger.warn("Unable to insert tuple, no connection to system: {}", system);
				return FutureHelper.getFailedEmptyResultFuture();
			}
			
			final RoutingHeader routingHeader = new RoutingHeader((short) 0, hops);
			
			return connection.insertTuple(table, tuple, routingHeader);
		} catch (ZookeeperException e) {
			throw new BBoxDBException(e);
		} catch (InterruptedException e) {
			logger.warn("Interrupted while waiting for systems list");
			Thread.currentThread().interrupt();
		}
		
		// Return after exception
		return FutureHelper.getFailedEmptyResultFuture();
	}

	@Override
	public EmptyResultFuture deleteTuple(final String table, final String key) throws BBoxDBException {
		final long timestamp = MicroSecondTimestampProvider.getNewTimestamp();
		return deleteTuple(table, key, timestamp);
	}
	
	@Override
	public EmptyResultFuture deleteTuple(final String table, final String key, final long timestamp) throws BBoxDBException {
		final List<BBoxDBClient> connections = membershipConnectionService.getAllConnections();
		
		if(membershipConnectionService.getNumberOfConnections() == 0) {
			throw new BBoxDBException("deleteTuple called, but connection list is empty");
		}
		
		final EmptyResultFuture future = new EmptyResultFuture();
				
		connections.stream()
		 	.map(c -> c.deleteTuple(table, key, timestamp))
		 	.filter(Objects::nonNull)
		 	.forEach(f -> future.merge(f));
		
		return future;
	}

	@Override
	public SSTableNameListFuture listTables() {
		try {
			final BBoxDBClient bboxDBClient = getSystemForNewRessources();
			return bboxDBClient.listTables();
		} catch (ResourceAllocationException e) {
			logger.warn("listTables called, but no ressoures are available", e);
			final SSTableNameListFuture future = new SSTableNameListFuture(1);
			future.setFailedState();
			future.fireCompleteEvent();
			return future;
		}
	}

	@Override
	public EmptyResultFuture createDistributionGroup(final String distributionGroup, final short replicationFactor) throws BBoxDBException {

		if(membershipConnectionService.getNumberOfConnections() == 0) {
			throw new BBoxDBException("createDistributionGroup called, but connection list is empty");
		}

		try {
			final BBoxDBClient bboxdbClient = getSystemForNewRessources();
			return bboxdbClient.createDistributionGroup(distributionGroup, replicationFactor);
		} catch (ResourceAllocationException e) {
			logger.warn("createDistributionGroup called, but no ressoures are available", e);
			return FutureHelper.getFailedEmptyResultFuture();
		}
	}

	/**
	 * Find a system with free resources
	 * @return
	 * @throws ResourceAllocationException 
	 */
	protected BBoxDBClient getSystemForNewRessources() throws ResourceAllocationException {
		final List<DistributedInstance> serverConnections = membershipConnectionService.getAllInstances();
		
		if(serverConnections == null) {
			throw new ResourceAllocationException("Server connections are null");
		}
		
		final DistributedInstance system = resourcePlacementStrategy.getInstancesForNewRessource(serverConnections);
		return membershipConnectionService.getConnectionForInstance(system);
	}

	@Override
	public EmptyResultFuture deleteDistributionGroup(final String distributionGroup) throws BBoxDBException {

		if(membershipConnectionService.getNumberOfConnections() == 0) {
			throw new BBoxDBException("deleteDistributionGroup called, but connection list is empty");
		}
		
		final EmptyResultFuture future = new EmptyResultFuture();

		membershipConnectionService.getAllConnections()
			.stream()
		 	.map(c -> c.deleteDistributionGroup(distributionGroup))
		 	.filter(Objects::nonNull)
		 	.forEach(f -> future.merge(f));

		return future;
	}

	@Override
	public TupleListFuture queryKey(final String table, final String key) throws BBoxDBException {
		if(membershipConnectionService.getNumberOfConnections() == 0) {
			throw new BBoxDBException("queryKey called, but connection list is empty");
		}
		
		final TupleListFuture future = new TupleListFuture();
		
		if(logger.isDebugEnabled()) {
			logger.debug("Query by for key {} in table {}", key, table);
		}

		membershipConnectionService.getAllConnections()
			.stream()
		 	.map(c -> c.queryKey(table, key))
		 	.filter(Objects::nonNull)
		 	.forEach(f -> future.merge(f));

		return future;
	}

	@Override
	public TupleListFuture queryBoundingBox(final String table, final BoundingBox boundingBox) throws BBoxDBException {
		if(membershipConnectionService.getNumberOfConnections() == 0) {
			throw new BBoxDBException("queryBoundingBox called, but connection list is empty");
		}
		
		final TupleListFuture future = new TupleListFuture();
		
		try {
			final SSTableName sstableName = new SSTableName(table);

			final KDtreeZookeeperAdapter distributionAdapter = DistributionGroupCache.getGroupForTableName(
					sstableName, zookeeperClient);

			final DistributionRegion distributionRegion = distributionAdapter.getRootNode();
			final Collection<RoutingHop> hops = distributionRegion.getRoutingHopsForRead(boundingBox);
			
			if(logger.isDebugEnabled()) {
				logger.debug("Query by for bounding box {} in table {} on systems {}", boundingBox, table, hops);
			}
			
			hops.stream()
				.map(s -> membershipConnectionService.getConnectionForInstance(s.getDistributedInstance()))
			 	.map(c -> c.queryBoundingBox(table, boundingBox))
			 	.filter(Objects::nonNull)
			 	.forEach(f -> future.merge(f));
			
		} catch (ZookeeperException e) {
			e.printStackTrace();
		}
		
		return future;
	}
	

	@Override
	public TupleListFuture queryBoundingBoxAndTime(final String table,
			final BoundingBox boundingBox, final long timestamp) throws BBoxDBException {
		
		if(membershipConnectionService.getNumberOfConnections() == 0) {
			throw new BBoxDBException("queryBoundingBoxAndTime called, but connection list is empty");
		}
		
		final TupleListFuture future = new TupleListFuture();
		
		try {
			final SSTableName sstableName = new SSTableName(table);
			final KDtreeZookeeperAdapter distributionAdapter = DistributionGroupCache.getGroupForTableName(
					sstableName, zookeeperClient);

			final DistributionRegion distributionRegion = distributionAdapter.getRootNode();
			final Collection<RoutingHop> hops = distributionRegion.getRoutingHopsForRead(boundingBox);
			
			if(logger.isDebugEnabled()) {
				logger.debug("Query by for bounding box {} in table {} on systems {}", boundingBox, table, hops);
			}
			
			hops.stream()
				.map(s -> membershipConnectionService.getConnectionForInstance(s.getDistributedInstance()))
			 	.map(c -> c.queryBoundingBoxAndTime(table, boundingBox, timestamp))
			 	.filter(Objects::nonNull)
			 	.forEach(f -> future.merge(f));
			
		} catch (ZookeeperException e) {
			e.printStackTrace();
		}
		
		return future;
	}

	@Override
	public TupleListFuture queryVersionTime(final String table, final long timestamp) throws BBoxDBException {
		if(membershipConnectionService.getNumberOfConnections() == 0) {
			throw new BBoxDBException("queryTime called, but connection list is empty");
		}
		
		if(logger.isDebugEnabled()) {
			logger.debug("Query by for timestamp {} in table {}", timestamp, table);
		}
		
		final TupleListFuture future = new TupleListFuture();

		membershipConnectionService.getAllConnections()
		 	.stream()
		 	.map(c -> c.queryVersionTime(table, timestamp))
		 	.filter(Objects::nonNull)
		 	.forEach(f -> future.merge(f));

		return future;
	}
	
	@Override
	public TupleListFuture queryInsertedTime(final String table, final long timestamp) throws BBoxDBException {
		if(membershipConnectionService.getNumberOfConnections() == 0) {
			throw new BBoxDBException("queryTime called, but connection list is empty");
		}
		
		if(logger.isDebugEnabled()) {
			logger.debug("Query by for timestamp {} in table {}", timestamp, table);
		}
		
		final TupleListFuture future = new TupleListFuture();
		
		membershipConnectionService.getAllConnections()
		 	.stream()
		 	.map(c -> c.queryInsertedTime(table, timestamp))
		 	.filter(Objects::nonNull)
		 	.forEach(f -> future.merge(f));

		return future;
	}

	@Override
	public boolean isConnected() {
		return (membershipConnectionService.getNumberOfConnections() > 0);
	}

	@Override
	public NetworkConnectionState getConnectionState() {
		return NetworkConnectionState.NETWORK_CONNECTION_OPEN;
	}

	@Override
	public int getInFlightCalls() {
		return membershipConnectionService
				.getAllConnections()
				.stream()
				.mapToInt(BBoxDBClient::getInFlightCalls)
				.sum();
	}

	@Override
	public short getMaxInFlightCalls() {
		return maxInFlightCalls;
	}

	@Override
	public void setMaxInFlightCalls(final short maxInFlightCalls) {
		this.maxInFlightCalls = maxInFlightCalls;
	}
	
	/**
	 * Is the paging for queries enables
	 * @return
	 */
	public boolean isPagingEnabled() {
		return membershipConnectionService.isPagingEnabled();
	}

	/**
	 * Enable or disable paging
	 * @param pagingEnabled
	 */
	public void setPagingEnabled(final boolean pagingEnabled) {
		membershipConnectionService.setPagingEnabled(pagingEnabled);
	}

	/**
	 * Get the amount of tuples per page
	 * @return
	 */
	public short getTuplesPerPage() {
		return membershipConnectionService.getTuplesPerPage();
	}

	/**
	 * Set the tuples per page
	 * @param tuplesPerPage
	 */
	public void setTuplesPerPage(final short tuplesPerPage) {
		membershipConnectionService.setTuplesPerPage(tuplesPerPage);
	}
	
}
