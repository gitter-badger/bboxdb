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
package org.bboxdb.storage.queryprocessor.datasource;

import java.util.Iterator;

import org.bboxdb.storage.ReadOnlyTupleStorage;
import org.bboxdb.storage.entity.BoundingBox;
import org.bboxdb.storage.entity.Tuple;

public class SpatialIndexDataSource implements DataSource {

	/**
	 * The tuple storage
	 */
	protected final ReadOnlyTupleStorage tupleStorage;
	
	/**
	 * The bounding box
	 */
	protected final BoundingBox boundingBox;
	
	public SpatialIndexDataSource(final ReadOnlyTupleStorage tupleStorage, 
			final BoundingBox boundingBox) {
		
		this.tupleStorage = tupleStorage;
		this.boundingBox = boundingBox;
	}

	@Override
	public Iterator<Tuple> iterator() {
		return tupleStorage.getAllTuplesInBoundingBox(boundingBox);
	}

}
