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
package org.bboxdb.misc;

import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

public class Const {
	
	/**
	 *  The version of the software
	 */
	public final static String VERSION = "0.3.2";
	
	/**
	 * The name of the configuration file
	 */
	public final static String CONFIG_FILE = "bboxdb.yaml";
	
	/**
	 * If an operation is retried, this is the max amount
	 * of retries
	 */
	public final static int OPERATION_RETRY = 20;
	
	/**
	 * The Byte order for encoded values
	 */
	public final static ByteOrder APPLICATION_BYTE_ORDER = ByteOrder.BIG_ENDIAN;
	
	/**
	 * The max timestamp delta between nodes (needed for recovery)
	 */
	public final static long MAX_NODE_CLOCK_DELTA = TimeUnit.SECONDS.toMicros(60);
	
	/**
	 * Max tuples in uncompressed queue
	 */
	public static final int MAX_UNCOMPRESSED_QUEUE_SIZE = 200;
}
