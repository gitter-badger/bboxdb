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
package org.bboxdb.distribution.mode;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.zookeeper.KeeperException;
import org.bboxdb.distribution.DistributionGroupName;
import org.bboxdb.distribution.DistributionRegion;
import org.bboxdb.distribution.DistributionRegionFactory;
import org.bboxdb.distribution.membership.DistributedInstance;
import org.bboxdb.distribution.zookeeper.ZookeeperClient;
import org.bboxdb.distribution.zookeeper.ZookeeperException;
import org.bboxdb.distribution.zookeeper.ZookeeperNodeNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributionGroupZookeeperAdapter {

	/**
	 * The zookeeper client
	 */
	protected final ZookeeperClient zookeeperClient;
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(DistributionGroupZookeeperAdapter.class);


	public DistributionGroupZookeeperAdapter(ZookeeperClient zookeeperClient) {
		this.zookeeperClient = zookeeperClient;
	}
	
	/**
	 * Get the next table id for a given distribution group
	 * @return
	 * @throws ZookeeperException 
	 */
	public int getNextTableIdForDistributionGroup(final String distributionGroup) throws ZookeeperException {
		
		final String distributionGroupIdQueuePath = getDistributionGroupIdQueuePath(distributionGroup);
		
		zookeeperClient.createDirectoryStructureRecursive(distributionGroupIdQueuePath);
	
		final String nodePath = distributionGroupIdQueuePath + "/" 
				+ ZookeeperNodeNames.SEQUENCE_QUEUE_PREFIX;
		
		final String nodename = zookeeperClient.createPersistentSequencialNode(
				nodePath, "".getBytes());
		
		// Delete the created node
		logger.debug("Got new table id; deleting node: {}", nodename);
		
		zookeeperClient.deleteNodesRecursive(nodename);
		
		// id-0000000063
		// Element 0: id-
		// Element 1: The number of the node
		final String[] splittedName = nodename.split(ZookeeperNodeNames.SEQUENCE_QUEUE_PREFIX);
		try {
			return Integer.parseInt(splittedName[1]);
		} catch(NumberFormatException e) {
			logger.warn("Unable to parse number: " + splittedName[1], e);
			throw new ZookeeperException(e);
		}

	}
	
	/**
	 * Read the structure of a distribution group
	 * @return
	 * @throws ZookeeperException 
	 */
	public KDtreeZookeeperAdapter readDistributionGroup(final String distributionGroup) throws ZookeeperException {
		final String path = getDistributionGroupPath(distributionGroup);

		if(! zookeeperClient.exists(path)) {
			final String exceptionMessage = MessageFormat.format("Unable to read {0}. Path {1} does not exist", distributionGroup, path);
			throw new ZookeeperException(exceptionMessage);
		}
		
		final DistributionRegion root = DistributionRegionFactory.createRootRegion(distributionGroup);

		readDistributionGroupRecursive(path, root);
		
		final KDtreeZookeeperAdapter kDtreeZookeeperAdapter = new KDtreeZookeeperAdapter(this, root);
		
		return kDtreeZookeeperAdapter;
	}
	
	/**
	 * Read the distribution group in a recursive way
	 * @param path
	 * @param child
	 * @throws ZookeeperException 
	 * @throws InterruptedException 
	 * @throws KeeperException 
	 */
	public void readDistributionGroupRecursive(final String path, final DistributionRegion child) throws ZookeeperException {
			
			final int namePrefix = zookeeperClient.getNamePrefixForPath(path);
			child.setNameprefix(namePrefix);

			child.setSystems(getSystemsForDistributionRegion(child));
			child.setState(getStateForDistributionRegion(path));

			// If the node is not split, stop recursion
			if(! isGroupSplitted(path)) {
				return;
			}
			
			final float splitFloat = getSplitPositionForPath(path);
			child.setSplit(splitFloat, false);
			
			readDistributionGroupRecursive(path + "/" + ZookeeperNodeNames.NAME_LEFT, child.getLeftChild());
			readDistributionGroupRecursive(path + "/" + ZookeeperNodeNames.NAME_RIGHT, child.getRightChild());
	}

	/**
	 * Get the split position for a given path
	 * @param path
	 * @return
	 * @throws ZookeeperException
	 */
	protected float getSplitPositionForPath(final String path) throws ZookeeperException  {
		
		final String splitPathName = path + "/" + ZookeeperNodeNames.NAME_SPLIT;
		String splitString = null;
		
		try {			
			splitString = zookeeperClient.readPathAndReturnString(splitPathName, false, null);
			return Float.parseFloat(splitString);
		} catch (NumberFormatException e) {
			throw new ZookeeperException("Unable to parse split pos '" + splitString + "' for " + splitPathName);
		}		
	}

	/**
	 * Test weather the group path is split or not
	 * @param path
	 * @return
	 * @throws ZookeeperException 
	 */
	protected boolean isGroupSplitted(final String path) throws ZookeeperException {

		final String splitPathName = path + "/" + ZookeeperNodeNames.NAME_SPLIT;
		
		if(! zookeeperClient.exists(splitPathName)) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Get the state for a given path
	 * @throws ZookeeperException 
	 */
	public NodeState getStateForDistributionRegion(final String path) throws ZookeeperException {
		final String statePath = path + "/" + ZookeeperNodeNames.NAME_STATE;
		final String state = zookeeperClient.readPathAndReturnString(statePath, false, null);
		return NodeState.fromString(state);
	}
	
	/**
	 * Get the state for a given path
	 * @return 
	 * @throws ZookeeperException 
	 */
	public NodeState getStateForDistributionRegion(final DistributionRegion region) throws ZookeeperException  {
		final String path = getZookeeperPathForDistributionRegion(region);
		return getStateForDistributionRegion(path);
	}
	
	/**
	 * Set the state for a given path
	 * @param path
	 * @param state
	 * @throws ZookeeperException 
	 */
	public void setStateForDistributionGroup(final String path, final NodeState state) throws ZookeeperException  {
		final String statePath = path + "/" + ZookeeperNodeNames.NAME_STATE;
		zookeeperClient.setData(statePath, state.getStringValue());
	}
	
	/**
	 * Set the state for a given distribution region
	 * @param region
	 * @param state
	 * @throws ZookeeperException
	 */
	public void setStateForDistributionGroup(final DistributionRegion region, final NodeState state) throws ZookeeperException  {
		final String path = getZookeeperPathForDistributionRegion(region);
		setStateForDistributionGroup(path, state);
	}

	/**
	 * Create a new distribution group
	 * @param distributionGroup
	 * @param replicationFactor
	 * @throws ZookeeperException 
	 */
	public void createDistributionGroup(final String distributionGroup, final short replicationFactor) throws ZookeeperException {
		
		final String path = getDistributionGroupPath(distributionGroup);
		
		zookeeperClient.createPersistentNode(path, "".getBytes());
		
		final int nameprefix = getNextTableIdForDistributionGroup(distributionGroup);
					
		zookeeperClient.createPersistentNode(path + "/" + ZookeeperNodeNames.NAME_NAMEPREFIX, 
				Integer.toString(nameprefix).getBytes());
		
		zookeeperClient.createPersistentNode(path + "/" + ZookeeperNodeNames.NAME_REPLICATION, 
				Short.toString(replicationFactor).getBytes());
		
		zookeeperClient.createPersistentNode(path + "/" + ZookeeperNodeNames.NAME_SYSTEMS, 
				"".getBytes());

		zookeeperClient.createPersistentNode(path + "/" + ZookeeperNodeNames.NAME_VERSION, 
				Long.toString(System.currentTimeMillis()).getBytes());
		
		zookeeperClient.createPersistentNode(path + "/" + ZookeeperNodeNames.NAME_STATE, 
				NodeState.ACTIVE.getStringValue().getBytes());

	}
	
	/**
	 * Get the zookeeper path for a distribution region
	 * @param distributionRegion
	 * @return
	 */
	public String getZookeeperPathForDistributionRegion(final DistributionRegion distributionRegion) {
		final String name = distributionRegion.getName();
		final StringBuilder sb = new StringBuilder();
		
		DistributionRegion tmpRegion = distributionRegion;
		while(tmpRegion.getParent() != null) {
			if(tmpRegion.isLeftChild()) {
				sb.insert(0, "/" + ZookeeperNodeNames.NAME_LEFT);
			} else {
				sb.insert(0, "/" + ZookeeperNodeNames.NAME_RIGHT);
			}
			
			tmpRegion = tmpRegion.getParent();
		}
		
		sb.insert(0, getDistributionGroupPath(name));
		return sb.toString();
	}
	
	
	/**
	 * Get the systems for the distribution region
	 * @param region
	 * @return
	 * @throws ZookeeperException 
	 */
	public Collection<DistributedInstance> getSystemsForDistributionRegion(final DistributionRegion region) throws ZookeeperException {
	
		final Set<DistributedInstance> result = new HashSet<DistributedInstance>();
		
		final String path = getZookeeperPathForDistributionRegion(region) 
				+ "/" + ZookeeperNodeNames.NAME_SYSTEMS;
		
		// Does the requested node exists?
		if(! zookeeperClient.exists(path)) {
			return null;
		}
		
		final List<String> childs = zookeeperClient.getChildren(path, null);
		
		for(final String childName : childs) {
			result.add(new DistributedInstance(childName));
		}
		
		return result;
	}
	

	
	/**
	 * Add a system to a distribution region
	 * @param region
	 * @param system
	 * @throws ZookeeperException 
	 */
	public void addSystemToDistributionRegion(final DistributionRegion region, final DistributedInstance system) throws ZookeeperException {
		
		if(system == null) {
			throw new IllegalArgumentException("Unable to add system with value null");
		}
	
		final String path = getZookeeperPathForDistributionRegion(region) 
				+ "/" + ZookeeperNodeNames.NAME_SYSTEMS;
		
		logger.debug("Register system under systems node: {}", path);
		
		zookeeperClient.createPersistentNode(path + "/" + system.getStringValue(), "".getBytes());
	}
	
	/**
	 * Set the checkpoint for the distribution region and system
	 * @param region
	 * @param system
	 * @throws ZookeeperException
	 * @throws InterruptedException 
	 */
	public void setCheckpointForDistributionRegion(final DistributionRegion region, final DistributedInstance system, final long checkpoint) throws ZookeeperException, InterruptedException {
		if(system == null) {
			throw new IllegalArgumentException("Unable to add system with value null");
		}
		
		final String path = getZookeeperPathForDistributionRegion(region) 
				+ "/" + ZookeeperNodeNames.NAME_SYSTEMS + "/" + system.getStringValue();
		
		logger.debug("Set checkpoint for: {} to {}", path, checkpoint);
		
		if(! zookeeperClient.exists(path)) {
			throw new ZookeeperException("Path " + path + " does not exists");
		}
		
		zookeeperClient.setData(path, Long.toString(checkpoint));
	}
	
	/**
	 * Get the checkpoint for the distribution region and system
	 * @param region
	 * @param system
	 * @return 
	 * @throws ZookeeperException
	 */
	public long getCheckpointForDistributionRegion(final DistributionRegion region, final DistributedInstance system) throws ZookeeperException {
		if(system == null) {
			throw new IllegalArgumentException("Unable to add system with value null");
		}
		
		try {
			final String path = getZookeeperPathForDistributionRegion(region) 
					+ "/" + ZookeeperNodeNames.NAME_SYSTEMS + "/" + system.getStringValue();
		
			if(! zookeeperClient.exists(path)) {
				throw new ZookeeperException("Path " + path + " does not exists");
			}

			final String checkpointString = zookeeperClient.getData(path);
			
			if("".equals(checkpointString)) {
				return -1;
			}
			
			return Long.parseLong(checkpointString);
		} catch (NumberFormatException e) {
			throw new ZookeeperException(e);
		}
	}
			
	/**
	 * Delete a system to a distribution region
	 * @param region
	 * @param system
	 * @return 
	 * @throws ZookeeperException 
	 */
	public boolean deleteSystemFromDistributionRegion(final DistributionRegion region, final DistributedInstance system) throws ZookeeperException {
		
		if(system == null) {
			throw new IllegalArgumentException("Unable to delete system with value null");
		}

		final String path = getZookeeperPathForDistributionRegion(region) + "/" + ZookeeperNodeNames.NAME_SYSTEMS + "/" + system.getStringValue();
		
		if(! zookeeperClient.exists(path)) {
			return false;
		}
		
		zookeeperClient.deleteNodesRecursive(path);
	
		return true;
	}
	

	/**
	 * Get the path for the distribution group id queue
	 * @param distributionGroup
	 * @return
	 */
	public String getDistributionGroupIdQueuePath(final String distributionGroup) {
		 return getDistributionGroupPath(distributionGroup) 
				 + "/" + ZookeeperNodeNames.NAME_PREFIXQUEUE;
	}
	
	/**
	 * Get the path for the distribution group
	 * @param distributionGroup
	 * @return
	 */
	public String getDistributionGroupPath(final String distributionGroup) {
		return zookeeperClient.getClusterPath() + "/" + distributionGroup;
	}

	/**
	 * Delete an existing distribution group
	 * @param distributionGroup
	 * @throws ZookeeperException 
	 */
	public void deleteDistributionGroup(final String distributionGroup) throws ZookeeperException {
		
		// Does the path not exist? We are done!
		if(! isDistributionGroupRegistered(distributionGroup)) {
			return;
		}
		
		final String path = getDistributionGroupPath(distributionGroup);			
		zookeeperClient.deleteNodesRecursive(path);
	}
	
	/**
	 * Does the distribution group exists?
	 * @param distributionGroup
	 * @return 
	 * @throws ZookeeperException
	 */
	public boolean isDistributionGroupRegistered(final String distributionGroup) throws ZookeeperException {
		final String path = getDistributionGroupPath(distributionGroup);

		if(! zookeeperClient.exists(path)) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * List all existing distribution groups
	 * @return
	 * @throws ZookeeperException 
	 */
	public List<DistributionGroupName> getDistributionGroups() throws ZookeeperException {
		final List<DistributionGroupName> groups = new ArrayList<DistributionGroupName>();
		final String clusterPath = zookeeperClient.getClusterPath();
		final List<String> nodes = zookeeperClient.getChildren(clusterPath, null);
		
		for(final String node : nodes) {
			final DistributionGroupName groupName = new DistributionGroupName(node);
			if(groupName.isValid()) {
				groups.add(groupName);
			} else {
				logger.warn("Got invalid distribution group name from zookeeper: " + groupName);
			}
		}
		
		return groups;
	}
	
	/**
	 * Get the replication factor for a distribution group
	 * @param distributionGroup
	 * @return
	 * @throws ZookeeperException
	 */
	public short getReplicationFactorForDistributionGroup(final String distributionGroup) throws ZookeeperException {
	
		final String path = getDistributionGroupPath(distributionGroup);
		final String fullPath = path + "/" + ZookeeperNodeNames.NAME_REPLICATION;
		final String data = zookeeperClient.getData(fullPath);
		
		try {
			return Short.parseShort(data);
		} catch (NumberFormatException e) {
			throw new ZookeeperException("Unable to parse replication factor: " + data + " for " + fullPath);
		}
	}
	
	/**
	 * Get the version number of the distribution group
	 * @param distributionGroup
	 * @return
	 * @throws ZookeeperException
	 */
	public String getVersionForDistributionGroup(final String distributionGroup) throws ZookeeperException {
		final String path = getDistributionGroupPath(distributionGroup);
		final String fullPath = path + "/" + ZookeeperNodeNames.NAME_VERSION;
		return zookeeperClient.readPathAndReturnString(fullPath, false, null);	 
	}
	
}