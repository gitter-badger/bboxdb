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
package org.bboxdb.network.server.handler.request;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.bboxdb.distribution.RegionIdMapper;
import org.bboxdb.distribution.RegionIdMapperInstanceManager;
import org.bboxdb.network.packages.PackageEncodeException;
import org.bboxdb.network.packages.request.DeleteTableRequest;
import org.bboxdb.network.packages.response.ErrorResponse;
import org.bboxdb.network.packages.response.SuccessResponse;
import org.bboxdb.network.server.ClientConnectionHandler;
import org.bboxdb.network.server.ErrorMessages;
import org.bboxdb.storage.StorageManagerException;
import org.bboxdb.storage.entity.SSTableName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleDeleteTable implements RequestHandler {
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(HandleDeleteTable.class);
	

	@Override
	/**
	 * Handle the delete table call
	 */
	public boolean handleRequest(final ByteBuffer encodedPackage, 
			final short packageSequence, final ClientConnectionHandler clientConnectionHandler) throws IOException, PackageEncodeException {
		
		if(logger.isDebugEnabled()) {
			logger.debug("Got delete table package");
		}
		
		try {			
			final DeleteTableRequest deletePackage = DeleteTableRequest.decodeTuple(encodedPackage);
			final SSTableName requestTable = deletePackage.getTable();
			logger.info("Got delete call for table: " + requestTable);
			
			// Send the call to the storage manager
			final RegionIdMapper regionIdMapper = RegionIdMapperInstanceManager.getInstance(requestTable.getDistributionGroupObject());
			final Collection<SSTableName> localTables = regionIdMapper.getAllLocalTables(requestTable);
			
			for(final SSTableName ssTableName : localTables) {
				clientConnectionHandler.getStorageRegistry().deleteTable(ssTableName);	
			}
			
			clientConnectionHandler.writeResultPackage(new SuccessResponse(packageSequence));
		} catch (StorageManagerException | PackageEncodeException e) {
			logger.warn("Error while delete tuple", e);

			final ErrorResponse responsePackage = new ErrorResponse(packageSequence, ErrorMessages.ERROR_EXCEPTION);
			clientConnectionHandler.writeResultPackage(responsePackage);
		}
		
		return true;
	}
}
