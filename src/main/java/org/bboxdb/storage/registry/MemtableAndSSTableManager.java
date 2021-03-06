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
package org.bboxdb.storage.registry;

import org.bboxdb.storage.memtable.Memtable;
import org.bboxdb.storage.sstable.SSTableManager;

public class MemtableAndSSTableManager {

	/**
	 * The memtale
	 */
	protected final Memtable memtable;
	
	/**
	 * The sstable manager
	 */
	protected final SSTableManager ssTableManager;

	public MemtableAndSSTableManager(final Memtable memtable, final SSTableManager ssTableManager) {
		this.memtable = memtable;
		this.ssTableManager = ssTableManager;
	}
	
	/**
	 * Get the memtable
	 * @return
	 */
	public Memtable getMemtable() {
		return memtable;
	}
	
	/**
	 * Get the sstable manager
	 * @return
	 */
	public SSTableManager getSsTableManager() {
		return ssTableManager;
	}
}
