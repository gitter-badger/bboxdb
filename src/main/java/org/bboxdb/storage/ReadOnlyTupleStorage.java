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
package org.bboxdb.storage;

import java.util.Iterator;

import org.bboxdb.storage.entity.BoundingBox;
import org.bboxdb.storage.entity.SSTableName;
import org.bboxdb.storage.entity.Tuple;

public interface ReadOnlyTupleStorage extends Iterable<Tuple> {
	
	/**
	 * Get the internal name of the tuple store
	 * @return
	 */
	public String getInternalName();

	/**
	 * Get the sstable name
	 * @return
	 */
	public SSTableName getSStableName();
	
	/**
	 * Search for tuple and return the most recent version
	 * @param key
	 * @return
	 * @throws StorageManagerException
	 */
	public Tuple get(final String key) throws StorageManagerException;

	/**
	 * Get all tuples that are inside the bounding box
	 * @param boundingBox
	 * @return
	 */
	public Iterator<Tuple> getAllTuplesInBoundingBox(final BoundingBox boundingBox);
	
	/**
	 * Get the number of tuples in the storage
	 * @return
	 */
	public long getNumberOfTuples();
	
	/**
	 * Get the n-th tuple
	 * @param position
	 * @return
	 * @throws StorageManagerException 
	 */
	public Tuple getTupleAtPosition(final long position) throws StorageManagerException;
	
	/**
	 * Get the version timestamp of the oldest tuple (in microseconds)
	 * @return
	 */
	public long getOldestTupleVersionTimestamp();
	
	/**
	 * Get the version timestamp of the newest tuple (in microseconds)
	 * @return
	 */
	public long getNewestTupleVersionTimestamp();
	
	/**
	 * Get the newest inserted timestamp (in microseconds)
	 * @return
	 */
	public long getNewestTupleInsertedTimestamp();
	
	/**
	 * Delete the object and persistent data as soon as usage == 0
	 */
	public void deleteOnClose();
	
	/**
	 * Is the deletion pending
	 * @return
	 */
	public boolean isDeletePending(); 

	/** 
	 * Increment the usage counter
	 * @return
	 */
	public abstract boolean acquire();

	/**
	 * Decrement the usage counter
	 */
	public abstract void release();
	
	/**
	 * Get the size of the storage
	 * @return
	 */
	public long getSize();
	
	/**
	 * Is the tuple store persistent or transient
	 * @return
	 */
	public boolean isPersistent();
	
}
