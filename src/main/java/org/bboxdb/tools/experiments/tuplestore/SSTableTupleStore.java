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
package org.bboxdb.tools.experiments.tuplestore;

import java.io.File;
import java.util.Arrays;

import org.bboxdb.misc.BBoxDBConfigurationManager;
import org.bboxdb.storage.entity.SSTableName;
import org.bboxdb.storage.entity.Tuple;
import org.bboxdb.storage.registry.StorageRegistry;
import org.bboxdb.storage.sstable.SSTableManager;

public class SSTableTupleStore implements TupleStore {

	/**
	 * The storage manager
	 */
	private SSTableManager storageManager;
	
	/**
	 * The database dir
	 */
	private File dir;
	
	/**
	 * The storage registry
	 */
	protected StorageRegistry storageRegistry;

	/**
	 * The sstable name
	 */
	protected final static SSTableName SSTABLE_NAME = new SSTableName("2_group1_test");


	public SSTableTupleStore(final File dir) {
		this.dir = dir;
	}

	@Override
	public void writeTuple(final Tuple tuple) throws Exception {
		storageManager.put(tuple);		
	}

	@Override
	public Tuple readTuple(final String key) throws Exception {
		final Tuple tuple = storageManager.get(key);
		
		if(tuple == null) {
			throw new RuntimeException("Unable to locate tuple for key: " + key);
		}
		
		return tuple;
	}

	@Override
	public void close() throws Exception {
		if(storageRegistry != null) {
			storageRegistry.shutdown();
			storageRegistry = null;
		}
	}

	@Override
	public void open() throws Exception {
		BBoxDBConfigurationManager.getConfiguration().setStorageDirectories(Arrays.asList(dir.getAbsolutePath()));		

		final File dataDir = new File(dir.getAbsoluteFile() + "/data");
		dataDir.mkdirs();
		
		storageRegistry = new StorageRegistry();
		storageRegistry.init();
		
		storageManager = storageRegistry.getSSTableManager(SSTABLE_NAME);
	}
}
