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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.bboxdb.network.client.Scalephant;
import org.bboxdb.network.client.ScalephantCluster;
import org.bboxdb.network.client.ScalephantException;
import org.bboxdb.network.client.future.EmptyResultFuture;
import org.bboxdb.network.client.future.TupleListFuture;
import org.bboxdb.storage.entity.BoundingBox;
import org.bboxdb.storage.entity.Tuple;


public class ScalephantClientExample {

	/**
	 * Connect to the Scalephant Server at localhost and insert some tuples
	 * 
	 * @param args
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @throws ScalephantException 
	 */
	public static void main(String[] args) throws InterruptedException, ExecutionException, ScalephantException {
		
		// A 2 dimensional table (member of distribution group 'mygroup3') with the name 'testdata'
		final String distributionGroup = "2_mygroup3"; 
		final String mytable = distributionGroup + "_testdata";
		
		// The name of the cluster
		final String clustername = "mycluster";
		
		// The zookeeper connect points
		final List<String> connectPoints = Arrays.asList("localhost:2181");
		
		// Connect to the server
		final Scalephant scalephantClient = new ScalephantCluster(connectPoints, clustername);
		scalephantClient.connect();
		
		// Check the connection state
		if (! scalephantClient.isConnected() ) {
			System.out.println("Error while connecting to the scalephant");
			System.exit(-1);
		}
		
		// Clean the old content of the distribution group
		final EmptyResultFuture deleteGroupResult = scalephantClient.deleteDistributionGroup(distributionGroup);
		deleteGroupResult.waitForAll();
		if(deleteGroupResult.isFailed()) {
			System.err.println("Unable to delete distribution group: " + distributionGroup);
			System.err.println(deleteGroupResult.getAllMessages());
			System.exit(-1);
		}
		
		// Create a new distribution group
		final EmptyResultFuture createGroupResult = scalephantClient.createDistributionGroup(distributionGroup, (short) 3);
		createGroupResult.waitForAll();
		if(createGroupResult.isFailed()) {
			System.err.println("Unable to create distribution group: " + distributionGroup);
			System.err.println(createGroupResult.getAllMessages());
			System.exit(-1);
		}
		
		// Insert two new tuples
		final Tuple tuple1 = new Tuple("key1", new BoundingBox(0f, 5f, 0f, 1f), "mydata1".getBytes());
		final EmptyResultFuture insertResult1 = scalephantClient.insertTuple(mytable, tuple1);
		
		final Tuple tuple2 = new Tuple("key2", new BoundingBox(-1f, 2f, -1f, 2f), "mydata2".getBytes());
		final EmptyResultFuture insertResult2 = scalephantClient.insertTuple(mytable, tuple2);
		
		// Wait for the insert operations to complete
		insertResult1.waitForAll();
		insertResult2.waitForAll();
		
		if(insertResult1.isFailed()) {
			System.err.println("Unable to insert tuple: " + insertResult1.getAllMessages());
			System.exit(-1);
		}
		
		if(insertResult2.isFailed()) {
			System.err.println("Unable to insert tuple: " + insertResult2.getAllMessages());
			System.exit(-1);
		}
		
		// Query by key
		final TupleListFuture resultFuture1 = scalephantClient.queryKey(mytable, "key");
		
		// We got a future object, the search is performed asynchronous
		// Wait for the result
		resultFuture1.waitForAll();
		
		if(resultFuture1.isFailed()) {
			System.err.println("Future is failed: " + resultFuture1.getAllMessages());
			System.exit(-1);
		}
		
		// Output all tuples
		for(final Tuple tuple : resultFuture1) {
			System.out.println(tuple);
		}
		
		// Query by bounding box
		final TupleListFuture resultFuture2 = scalephantClient.queryBoundingBox(mytable, new BoundingBox(-0.5f, 1f, -0.5f, 1f));
		
		// Again, we got a future object, the search is performed asynchronous
		resultFuture2.waitForAll();
		
		if(resultFuture2.isFailed()) {
			System.err.println("Future is failed: " + resultFuture2.getAllMessages());
			System.exit(-1);
		}
		
		// Output all tuples
		for(final Tuple tuple : resultFuture2) {
			System.out.println("Tuple: " + tuple);
		}

		scalephantClient.disconnect();
	}
	
}
