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
package org.bboxdb.distribution.zookeeper;

public class ZookeeperNodeNames {
	/**
	 * The prefix for nodes in sequential queues
	 */
	public final static String SEQUENCE_QUEUE_PREFIX = "id-";
	
	/**
	 * Name of the left tree node
	 */
	public final static String NAME_LEFT = "left";
	
	/**
	 * Name of the right tree node
	 */
	public final static String NAME_RIGHT = "right";
	
	/**
	 * Name of the split node
	 */
	public final static String NAME_SPLIT = "split";
	
	/**
	 * Name of the name prefix node
	 */
	public final static String NAME_NAMEPREFIX = "nameprefix";
	
	/**
	 * Name of the name prefix queue
	 */
	public static final String NAME_PREFIXQUEUE = "nameprefixqueue";

	/**
	 * Name of the replication node
	 */
	public final static String NAME_REPLICATION = "replication";
	
	/**
	 * Name of the systems node
	 */
	public final static String NAME_SYSTEMS = "systems";
	
	/**
	 * Name of the version node
	 */
	public final static String NAME_VERSION = "version";
	
	/**
	 * Name of the state node
	 */
	public final static String NAME_STATE = "state";
}