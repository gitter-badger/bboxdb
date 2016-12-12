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
package org.bboxdb.storage;

import org.bboxdb.storage.entity.BoundingBox;
import org.bboxdb.storage.entity.Tuple;
import org.bboxdb.storage.sstable.TupleHelper;
import org.junit.Assert;
import org.junit.Test;

public class TestTupleHelper {

	@Test
	public void testGetMostRecentTuple() {
		final Tuple tupleA = new Tuple("abc", BoundingBox.EMPTY_BOX, null, 1);
		final Tuple tupleB = new Tuple("abc", BoundingBox.EMPTY_BOX, null, 2);
		
		Assert.assertEquals(null, TupleHelper.returnMostRecentTuple(null, null));
		Assert.assertEquals(tupleA, TupleHelper.returnMostRecentTuple(tupleA, null));
		Assert.assertEquals(tupleA, TupleHelper.returnMostRecentTuple(null, tupleA));

		Assert.assertEquals(tupleB, TupleHelper.returnMostRecentTuple(tupleA, tupleB));
		Assert.assertEquals(tupleB, TupleHelper.returnMostRecentTuple(tupleB, tupleA));
		Assert.assertEquals(tupleB, TupleHelper.returnMostRecentTuple(tupleB, tupleB));
	}

}