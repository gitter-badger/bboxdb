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
package org.bboxdb.network.server.handler.query;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.bboxdb.distribution.RegionIdMapper;
import org.bboxdb.distribution.RegionIdMapperInstanceManager;
import org.bboxdb.network.packages.PackageEncodeException;
import org.bboxdb.network.packages.request.QueryKeyRequest;
import org.bboxdb.network.packages.response.ErrorResponse;
import org.bboxdb.network.packages.response.SuccessResponse;
import org.bboxdb.network.server.ClientConnectionHandler;
import org.bboxdb.network.server.ErrorMessages;
import org.bboxdb.storage.entity.SSTableName;
import org.bboxdb.storage.entity.Tuple;
import org.bboxdb.storage.sstable.SSTableManager;
import org.bboxdb.util.concurrent.ExceptionSafeThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleKeyQuery implements QueryHandler {
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(HandleKeyQuery.class);
	

	@Override
	/**
	 * Handle a key query
	 */
	public void handleQuery(final ByteBuffer encodedPackage, 
			final short packageSequence, final ClientConnectionHandler clientConnectionHandler) 
					throws IOException, PackageEncodeException {

		final Runnable queryRunable = new ExceptionSafeThread() {

			@Override
			public void runThread() throws Exception {
				final QueryKeyRequest queryKeyRequest = QueryKeyRequest.decodeTuple(encodedPackage);
				final SSTableName requestTable = queryKeyRequest.getTable();
				
				// Send the call to the storage manager
				final RegionIdMapper regionIdMapper = RegionIdMapperInstanceManager.getInstance(requestTable.getDistributionGroupObject());
				final Collection<SSTableName> localTables = regionIdMapper.getAllLocalTables(requestTable);
				
				for(final SSTableName ssTableName : localTables) {
					
					final SSTableManager storageManager = clientConnectionHandler
							.getStorageRegistry()
							.getSSTableManager(ssTableName);
					
					final Tuple tuple = storageManager.get(queryKeyRequest.getKey());
					
					if(tuple != null) {
						clientConnectionHandler.writeResultTuple(packageSequence, requestTable, tuple);
						return;
					}
				}

				clientConnectionHandler.writeResultPackage(new SuccessResponse(packageSequence));
				return;
			}			
			
			@Override
			protected void afterExceptionHook() {
				final ErrorResponse responsePackage = new ErrorResponse(packageSequence, ErrorMessages.ERROR_EXCEPTION);
				clientConnectionHandler.writeResultPackageNE(responsePackage);	
			}
		};

		// Submit the runnable to our pool
		if(clientConnectionHandler.getThreadPool().isShutdown()) {
			logger.warn("Thread pool is shutting down, don't execute query: {}", packageSequence);
			final ErrorResponse responsePackage = new ErrorResponse(packageSequence, ErrorMessages.ERROR_QUERY_SHUTDOWN);
			clientConnectionHandler.writeResultPackage(responsePackage);
		} else {
			clientConnectionHandler.getThreadPool().submit(queryRunable);
		}		
	}
}
