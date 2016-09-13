package de.fernunihagen.dna.scalephant.network.packages.request;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import de.fernunihagen.dna.scalephant.Const;
import de.fernunihagen.dna.scalephant.network.NetworkConst;
import de.fernunihagen.dna.scalephant.network.NetworkPackageDecoder;
import de.fernunihagen.dna.scalephant.network.NetworkPackageEncoder;
import de.fernunihagen.dna.scalephant.network.packages.NetworkRequestPackage;
import de.fernunihagen.dna.scalephant.network.packages.PackageEncodeError;
import de.fernunihagen.dna.scalephant.network.routing.RoutingHeader;

public class CompressionEnvelopeRequest implements NetworkRequestPackage {
	
	/**
	 * The package to encode
	 */
	protected NetworkRequestPackage networkRequestPackage;
	
	/**
	 * The compression type
	 */
	protected byte compressionType;

	public CompressionEnvelopeRequest(final NetworkRequestPackage networkRequestPackage, final byte compressionType) {
		this.networkRequestPackage = networkRequestPackage;
		this.compressionType = compressionType;
	}

	public void writeToOutputStream(final short sequenceNumber, final OutputStream outputStream) throws PackageEncodeError {
		try {
			if(compressionType != NetworkConst.COMPRESSION_TYPE_GZIP) {
				throw new PackageEncodeError("Unknown compression method: " + compressionType);
			}
			
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final OutputStream os = new GZIPOutputStream(baos);
			networkRequestPackage.writeToOutputStream(sequenceNumber, os);
			os.close();
			final byte[] compressedBytes = baos.toByteArray();
			
			final ByteBuffer bb = ByteBuffer.allocate(5);
			bb.order(Const.APPLICATION_BYTE_ORDER);
			bb.putInt(compressedBytes.length);
			bb.put(compressionType);
			
			// Body length
			final long bodyLength = bb.capacity() + compressedBytes.length;

			// Unrouted package
			final RoutingHeader routingHeader = new RoutingHeader(false);
			NetworkPackageEncoder.appendRequestPackageHeader(sequenceNumber, bodyLength, 
					routingHeader, getPackageType(), outputStream);
			
			// Write body
			outputStream.write(bb.array());
			outputStream.write(compressedBytes);

		} catch (IOException e) {
			throw new PackageEncodeError("Got an IO Exception while writing compressed data");
		}
	}

	/**
	 * Decode the encoded package into a uncompressed byte stream 
	 * 
	 * @param encodedPackage
	 * @return
	 * @throws IOException 
	 * @throws PackageEncodeError 
	 */
	public static byte[] decodePackage(final ByteBuffer encodedPackage) throws PackageEncodeError {
		final boolean decodeResult = NetworkPackageDecoder.validateRequestPackageHeader(encodedPackage, NetworkConst.REQUEST_TYPE_COMPRESSION);
		
		if(decodeResult == false) {
			throw new PackageEncodeError("Unable to decode package");
		}
		
		final int compressedDataLength = encodedPackage.getInt();
		final byte compressionType = encodedPackage.get();
		
		if(compressionType != NetworkConst.COMPRESSION_TYPE_GZIP) {
			throw new PackageEncodeError("Unknown compression type: " + compressionType);
		}
		
		if(compressedDataLength != encodedPackage.remaining()) {
			throw new PackageEncodeError("Remaning : " + encodedPackage.remaining() + " bytes. But compressed data should have: " + compressedDataLength + " bytes");
		}
		
		final byte[] compressedBytes = new byte[compressedDataLength];
		encodedPackage.get(compressedBytes, 0, compressedDataLength);
		
		ByteArrayOutputStream baos;
		try {
			final ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
			final GZIPInputStream inputStream = new GZIPInputStream(bais);
			baos = new ByteArrayOutputStream();

			final byte[] buffer = new byte[10240];
			for (int length = 0; (length = inputStream.read(buffer)) > 0; ) {
				baos.write(buffer, 0, length);
			}

			inputStream.close();
			baos.close();
			return baos.toByteArray();
		} catch (IOException e) {
			throw new PackageEncodeError(e);
		}
		
	}
	
	@Override
	public byte getPackageType() {
		return NetworkConst.REQUEST_TYPE_COMPRESSION;
	}
	
}