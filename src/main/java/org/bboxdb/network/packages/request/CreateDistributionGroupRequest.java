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
package org.bboxdb.network.packages.request;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.bboxdb.misc.Const;
import org.bboxdb.network.NetworkConst;
import org.bboxdb.network.NetworkPackageDecoder;
import org.bboxdb.network.packages.NetworkRequestPackage;
import org.bboxdb.network.packages.PackageEncodeException;
import org.bboxdb.network.routing.RoutingHeader;

public class CreateDistributionGroupRequest extends NetworkRequestPackage {
	
	/**
	 * The name of the table
	 */
	protected final String distributionGroup;
	
	/**
	 * The replication factor for the distribution group
	 */
	protected final short replicationFactor;

	public CreateDistributionGroupRequest(final short sequencNumber,
			final String distributionGroup, final short replicationFactor) {
		
		super(sequencNumber);
		
		this.distributionGroup = distributionGroup;
		this.replicationFactor = replicationFactor;
	}
	
	@Override
	public void writeToOutputStream(final OutputStream outputStream) throws PackageEncodeException {

		try {
			final byte[] groupBytes = distributionGroup.getBytes();
			
			final ByteBuffer bb = ByteBuffer.allocate(4);
			bb.order(Const.APPLICATION_BYTE_ORDER);
			bb.putShort((short) groupBytes.length);
			bb.putShort(replicationFactor);
			
			// Body length
			final long bodyLength = bb.capacity() + groupBytes.length;

			// Unrouted package
			final RoutingHeader routingHeader = new RoutingHeader(false);
			appendRequestPackageHeader(bodyLength, routingHeader, outputStream);
			
			// Write body
			outputStream.write(bb.array());
			outputStream.write(groupBytes);			
		} catch (IOException e) {
			throw new PackageEncodeException("Got exception while converting package into bytes", e);
		}
	}
	
	/**
	 * Decode the encoded package into a object
	 * 
	 * @param encodedPackage
	 * @return
	 * @throws PackageEncodeException 
	 */
	public static CreateDistributionGroupRequest decodeTuple(final ByteBuffer encodedPackage) throws PackageEncodeException {
		final short sequenceNumber = NetworkPackageDecoder.getRequestIDFromRequestPackage(encodedPackage);

		final boolean decodeResult = NetworkPackageDecoder.validateRequestPackageHeader(encodedPackage, NetworkConst.REQUEST_TYPE_CREATE_DISTRIBUTION_GROUP);
		
		if(decodeResult == false) {
			throw new PackageEncodeException("Unable to decode package");
		}
		
		final short groupLength = encodedPackage.getShort();
		final short replicationFactor = encodedPackage.getShort();
		
		final byte[] groupBytes = new byte[groupLength];
		encodedPackage.get(groupBytes, 0, groupBytes.length);
		final String distributionGroup = new String(groupBytes);
		
		if(encodedPackage.remaining() != 0) {
			throw new PackageEncodeException("Some bytes are left after decoding: " + encodedPackage.remaining());
		}
		
		return new CreateDistributionGroupRequest(sequenceNumber, distributionGroup, replicationFactor);
	}

	@Override
	public byte getPackageType() {
		return NetworkConst.REQUEST_TYPE_CREATE_DISTRIBUTION_GROUP;
	}

	public String getDistributionGroup() {
		return distributionGroup;
	}
	
	public short getReplicationFactor() {
		return replicationFactor;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((distributionGroup == null) ? 0 : distributionGroup.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CreateDistributionGroupRequest other = (CreateDistributionGroupRequest) obj;
		if (distributionGroup == null) {
			if (other.distributionGroup != null)
				return false;
		} else if (!distributionGroup.equals(other.distributionGroup))
			return false;
		return true;
	}

}
