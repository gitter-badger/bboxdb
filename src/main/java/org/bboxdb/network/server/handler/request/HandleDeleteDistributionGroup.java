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

import org.bboxdb.distribution.DistributionGroupName;
import org.bboxdb.distribution.mode.DistributionGroupZookeeperAdapter;
import org.bboxdb.distribution.zookeeper.ZookeeperClientFactory;
import org.bboxdb.network.packages.PackageEncodeException;
import org.bboxdb.network.packages.request.DeleteDistributionGroupRequest;
import org.bboxdb.network.packages.response.ErrorResponse;
import org.bboxdb.network.packages.response.SuccessResponse;
import org.bboxdb.network.server.ClientConnectionHandler;
import org.bboxdb.network.server.ErrorMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleDeleteDistributionGroup implements RequestHandler {
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(HandleDeleteDistributionGroup.class);
	

	@Override
	/**
	 * Delete an existing distribution group
	 */
	public boolean handleRequest(final ByteBuffer encodedPackage, 
			final short packageSequence, final ClientConnectionHandler clientConnectionHandler) throws IOException, PackageEncodeException {
		
		if(logger.isDebugEnabled()) {
			logger.debug("Got delete distribution group package");
		}
		
		try {
			final DeleteDistributionGroupRequest deletePackage = DeleteDistributionGroupRequest.decodeTuple(encodedPackage);
			logger.info("Delete distribution group: " + deletePackage.getDistributionGroup());
			
			// Delete in Zookeeper
			final DistributionGroupZookeeperAdapter distributionGroupZookeeperAdapter = ZookeeperClientFactory.getDistributionGroupAdapter();
			distributionGroupZookeeperAdapter.deleteDistributionGroup(deletePackage.getDistributionGroup());
			
			// Delete local stored data
			final DistributionGroupName distributionGroupName = new DistributionGroupName(deletePackage.getDistributionGroup());
			clientConnectionHandler.getStorageRegistry().deleteAllTablesInDistributionGroup(distributionGroupName);
			
			clientConnectionHandler.writeResultPackage(new SuccessResponse(packageSequence));
		} catch (Exception e) {
			logger.warn("Error while delete distribution group", e);
			
			final ErrorResponse responsePackage = new ErrorResponse(packageSequence, ErrorMessages.ERROR_EXCEPTION);
			clientConnectionHandler.writeResultPackage(responsePackage);
		}
		
		return true;
	}
}
