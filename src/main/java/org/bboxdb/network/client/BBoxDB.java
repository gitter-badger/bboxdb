/*******************************************************************************
 *
 *    Copyright (C) 2015-2016
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

import org.bboxdb.network.NetworkConnectionState;
import org.bboxdb.network.client.future.EmptyResultFuture;
import org.bboxdb.network.client.future.SSTableNameListFuture;
import org.bboxdb.network.client.future.TupleListFuture;
import org.bboxdb.storage.entity.BoundingBox;
import org.bboxdb.storage.entity.Tuple;

public interface BBoxDB {
	
	/**
	 * The maximum amount of in flight requests. Needs to be lower than Short.MAX_VALUE to
	 * prevent two in flight requests with the same id.
	 */
	public final static short MAX_IN_FLIGHT_CALLS = 1000;

	/**
	 * Connect to the server
	 * @return true or false, depending on the connection state
	 */
	public boolean connect();

	/**
	 * Disconnect from the server
	 */
	public void disconnect();

	/**
	 * Delete a table on the bboxdb server
	 * @param table
	 * @return
	 */
	public EmptyResultFuture deleteTable(final String table) throws BBoxDBException;

	/**
	 * Insert a new tuple into the given table
	 * @param tuple
	 * @param table
	 * @return
	 */
	public EmptyResultFuture insertTuple(final String table, final Tuple tuple) throws BBoxDBException;

	/**
	 * Delete the given key from a table
	 * @param table
	 * @param key
	 * @param timestamp
	 * @return
	 */
	public EmptyResultFuture deleteTuple(final String table, final String key, final long timestamp) throws BBoxDBException;

	/**
	 * List the existing tables
	 * @return
	 */
	public SSTableNameListFuture listTables() throws BBoxDBException;

	/**
	 * Create a new distribution group
	 * @param distributionGroup
	 * @return
	 */
	public EmptyResultFuture createDistributionGroup(
			final String distributionGroup, final short replicationFactor) throws BBoxDBException;

	/**
	 * Delete a distribution group
	 * @param distributionGroup
	 * @return
	 */
	public EmptyResultFuture deleteDistributionGroup(
			final String distributionGroup) throws BBoxDBException;

	/**
	 * Query the given table for a specific key
	 * @param table
	 * @param key
	 * @return
	 */
	public TupleListFuture queryKey(final String table, final String key) throws BBoxDBException;

	/**
	 * Execute a bounding box query on the given table
	 * @param table
	 * @param boundingBox
	 * @return
	 */
	public TupleListFuture queryBoundingBox(final String table,
			final BoundingBox boundingBox) throws BBoxDBException;

	/**
	 * Query the given table for all tuples newer than timestamp
	 * @param table
	 * @param key
	 * @return
	 */
	public TupleListFuture queryTime(final String table, final long timestamp) throws BBoxDBException;

	/**
	 * Query the given table for all tuples newer than timestamp and inside of the bounding box
	 * @param table
	 * @param key
	 * @return
	 */
	public TupleListFuture queryBoundingBoxAndTime(final String table, final BoundingBox boundingBox, final long timestamp) throws BBoxDBException;

	/**
	 * Is the client connected?
	 * @return
	 */
	public boolean isConnected();

	/**
	 * Returns the state of the connection
	 * @return
	 */
	public NetworkConnectionState getConnectionState();

	/**
	 * Get the amount of in flight (running) calls
	 * @return
	 */
	public int getInFlightCalls();

	/**
	 * Get the max amount of in flight calls
	 * @return
	 */
	public short getMaxInFlightCalls();

	/**
	 * Set the max amount of in flight calls
	 * @param maxInFlightCalls
	 */
	public void setMaxInFlightCalls(final short maxInFlightCalls);
	
	/**
	 * Is the paging for queries enables
	 * @return
	 */
	public boolean isPagingEnabled();

	/**
	 * Enable or disable paging
	 * @param pagingEnabled
	 */
	public void setPagingEnabled(final boolean pagingEnabled);

	/**
	 * Get the amount of tuples per page
	 * @return
	 */
	public short getTuplesPerPage();

	/**
	 * Set the tuples per page
	 * @param tuplesPerPage
	 */
	public void setTuplesPerPage(final short tuplesPerPage);

}