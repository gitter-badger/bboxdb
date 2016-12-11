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
package de.fernunihagen.dna.scalephant.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fernunihagen.dna.scalephant.network.client.ScalephantCluster;
import de.fernunihagen.dna.scalephant.network.client.ScalephantException;
import de.fernunihagen.dna.scalephant.network.client.future.EmptyResultFuture;
import de.fernunihagen.dna.scalephant.network.client.future.TupleListFuture;
import de.fernunihagen.dna.scalephant.storage.entity.BoundingBox;
import de.fernunihagen.dna.scalephant.storage.entity.Tuple;
import de.fernunihagen.dna.scalephant.storage.queryprocessor.IteratorHelper;

public class DistributedSelftest {

	/**
	 * The name of the distribution group
	 */
	private static final String DISTRIBUTION_GROUP = "2_testgroup";
	
	/**
	 * The table to query
	 */
	private static final String TABLE = DISTRIBUTION_GROUP + "_mytable";
	
	/**
	 * The amount of operations
	 */
	private static final int NUMBER_OF_OPERATIONS = 10000;
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(DistributedSelftest.class);


	public static void main(final String[] args) throws InterruptedException, ExecutionException, ScalephantException {
		
		if(args.length < 2) {
			logger.error("Usage: DistributedSelftest <Cluster-Name> <Cluster-Endpoint1> <Cluster-EndpointN>");
			System.exit(-1);
		}

		logger.info("Running selftest......");
		
		final String clustername = args[0];
		final Collection<String> endpoints = new ArrayList<String>();
		for(int i = 1; i < args.length; i++) {
			endpoints.add(args[i]);
		}
	
		final ScalephantCluster scalephantCluster = new ScalephantCluster(endpoints, clustername); 
		scalephantCluster.connect();
		
		if(! scalephantCluster.isConnected()) {
			logger.error("Connection could not be established");
			System.exit(-1);
		}
		
		logger.info("Connected to cluster: " + clustername);
		logger.info("With endpoint(s): " + endpoints);
		
		recreateDistributionGroup(scalephantCluster);
		
		executeSelftest(scalephantCluster);
	}

	/**
	 * Recreate the distribution group	
	 * @param scalephantCluster
	 * @throws ScalephantException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private static void recreateDistributionGroup(final ScalephantCluster scalephantCluster) throws ScalephantException, InterruptedException, ExecutionException {
		logger.info("Delete old distribution group: " + DISTRIBUTION_GROUP);
		final EmptyResultFuture deleteFuture = scalephantCluster.deleteDistributionGroup(DISTRIBUTION_GROUP);
		deleteFuture.waitForAll();
		if(deleteFuture.isFailed()) {
			logger.error("Unable to delete distribution group: " + DISTRIBUTION_GROUP);
			logger.error(deleteFuture.getAllMessages());
			System.exit(-1);
		}
		
		// Wait for distribution group to settle
		Thread.sleep(5000);
		
		logger.info("Create new distribution group: " + DISTRIBUTION_GROUP);
		final EmptyResultFuture createFuture = scalephantCluster.createDistributionGroup(DISTRIBUTION_GROUP, (short) 2);
		createFuture.waitForAll();
		if(createFuture.isFailed()) {
			logger.error("Unable to create distribution group: " + DISTRIBUTION_GROUP);
			logger.error(createFuture.getAllMessages());
			System.exit(-1);
		}
		
		// Wait for distribution group to appear
		Thread.sleep(5000);
	}

	/**
	 * Execute the selftest
	 * @param scalephantClient
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws ScalephantException 
	 */
	private static void executeSelftest(final ScalephantCluster scalephantClient) throws InterruptedException, ExecutionException, ScalephantException {
		final Random random = new Random();
		long iteration = 1;
		
		while(true) {
			logger.info("Starting new iteration: " + iteration);
			insertNewTuples(scalephantClient);
			queryForExistingTuplesByKey(scalephantClient, random);
			queryForExistingTuplesByTime(scalephantClient);
			deleteTuples(scalephantClient);
			queryForNonExistingTuples(scalephantClient);
			
			Thread.sleep(1000);
			
			iteration++;
		}
	}

	/**
	 * Execute a time query
	 * @param scalephantClient
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @throws ScalephantException 
	 */
	private static void queryForExistingTuplesByTime(final ScalephantCluster scalephantClient) throws InterruptedException, ExecutionException, ScalephantException {
		logger.info("Executing time query");
		
		final TupleListFuture queryResult = scalephantClient.queryTime(TABLE, 0);
		queryResult.waitForAll();
		
		if(queryResult.isFailed()) {
			logger.error("Time query result is failed");
			logger.error(queryResult.getAllMessages());
			System.exit(-1);
		}
		
		final int totalTuples = IteratorHelper.getIteratorSize(queryResult.iterator());
		
		if(totalTuples != NUMBER_OF_OPERATIONS) {
			logger.error("Got {} tuples back, but expected {}", totalTuples, NUMBER_OF_OPERATIONS);
			System.exit(-1);
		}
	}

	/**
	 * Delete the stored tuples
	 * @param scalephantClient
	 * @throws InterruptedException
	 * @throws ScalephantException 
	 * @throws ExecutionException 
	 */
	private static void deleteTuples(final ScalephantCluster scalephantClient) throws InterruptedException, ScalephantException, ExecutionException {
		logger.info("Deleting tuples");
		for(int i = 0; i < NUMBER_OF_OPERATIONS; i++) {
			final String key = Integer.toString(i);
			final EmptyResultFuture deletionResult = scalephantClient.deleteTuple(TABLE, key, System.currentTimeMillis());
			deletionResult.waitForAll();
			
			if(deletionResult.isFailed() ) {
				logger.error("Got an error while deleting: {} ", key);
				logger.error(deletionResult.getAllMessages());
				System.exit(-1);
			}
		}
	}

	/**
	 * Query for the stored tuples
	 * @param scalephantClient
	 * @param random
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws ScalephantException 
	 */
	private static void queryForExistingTuplesByKey(final ScalephantCluster scalephantClient, final Random random) throws InterruptedException, ExecutionException, ScalephantException {
		logger.info("Query for tuples");
		for(int i = 0; i < NUMBER_OF_OPERATIONS; i++) {
			final String key = Integer.toString(Math.abs(random.nextInt()) % NUMBER_OF_OPERATIONS);
			final TupleListFuture queryResult = scalephantClient.queryKey(TABLE, key);
			queryResult.waitForAll();
			
			if(queryResult.isFailed()) {
				logger.error("Query {} : Got failed future, when query for: {}", i, key);
				logger.error(queryResult.getAllMessages());
				System.exit(-1);
			}
			
			boolean tupleFound = false;
			
			for(final Tuple tuple : queryResult) {
				if(! tuple.getKey().equals(key)) {
					logger.error("Query {}: Got tuple with wrong key.", i);
					logger.error("Expected: {} but got: {}", i, tuple.getKey());
					System.exit(-1);
				}
				
				tupleFound = true;
			}
			
			if(tupleFound == false) {
				logger.error("Query {}: Key {} not found", i, key);
				System.exit(-1);
			}
		}
	}
	
	/**
	 * Query for non existing tuples and exit, as soon as a tuple is found
	 * @param scalephantClient
	 * @throws ScalephantException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private static void queryForNonExistingTuples(final ScalephantCluster scalephantClient) throws ScalephantException, InterruptedException, ExecutionException {
		logger.info("Query for non existing tuples");
		
		for(int i = 0; i < NUMBER_OF_OPERATIONS; i++) {
			final String key = Integer.toString(i);
	
			final TupleListFuture queryResult = scalephantClient.queryKey(TABLE, key);
			queryResult.waitForAll();
			
			if(queryResult.isFailed()) {
				logger.error("Query {}: Got failed future, when query for: {}", i, key);
				logger.error(queryResult.getAllMessages());
				System.exit(-1);
			}
			
			for (final Tuple tuple : queryResult) {
				logger.error("Found a tuple which should not exist: {} / {}", i, tuple);
				System.exit(-1);
			}
		}
	}

	/**
	 * Insert new tuples
	 * @param scalephantClient
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws ScalephantException 
	 */
	private static void insertNewTuples(final ScalephantCluster scalephantClient) throws InterruptedException, ExecutionException, ScalephantException {
		logger.info("Inserting new tuples");
		
		for(int i = 0; i < NUMBER_OF_OPERATIONS; i++) {
			final String key = Integer.toString(i);
			final Tuple myTuple = new Tuple(key, new BoundingBox(1.0f, 2.0f, 1.0f, 2.0f), "test".getBytes());
			final EmptyResultFuture insertResult = scalephantClient.insertTuple(TABLE, myTuple);
			insertResult.waitForAll();
			
			if(insertResult.isFailed()) {
				logger.error("Got an error during tuple insert: ", insertResult.getAllMessages());
				System.exit(-1);
			}
		}
	}
}