package de.fernunihagen.dna.scalephant.network.routing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fernunihagen.dna.scalephant.network.NetworkHelper;
import de.fernunihagen.dna.scalephant.tools.DataEncoderHelper;

public class RoutingHeaderParser {
	
	/**
	 * The Logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(RoutingHeaderParser.class);
	
	/**
	 * Decode the routing header from a input stream
	 * @param bb
	 * @return
	 * @throws IOException 
	 */
	public static RoutingHeader decodeRoutingHeader(final InputStream inputStream) throws IOException {
		
		final byte[] routedOrDirect = new byte[1];
		inputStream.read(routedOrDirect, 0, 1);
		
		if(        (routedOrDirect[0] != RoutingHeader.DIRECT_PACKAGE) 
				&& (routedOrDirect[0] != RoutingHeader.ROUTED_PACKAGE)) {
			
			logger.error("Invalid package type, unable to decode package header: " + routedOrDirect);
			return null;
		}
		
		if(routedOrDirect[0] == RoutingHeader.DIRECT_PACKAGE) {
			return decodeDirectPackage(inputStream);
		} else {
			return decodeRoutedPackage(inputStream);
		} 
	}
	
	/**
	 * Read the routing header from a byte buffer
	 * @param bb
	 * @return
	 * @throws IOException
	 */
	public static RoutingHeader decodeRoutingHeader(final ByteBuffer bb) throws IOException {
		final ByteArrayInputStream bis = new ByteArrayInputStream(bb.array(), bb.position(), bb.remaining());
				
		final RoutingHeader routingHeader = decodeRoutingHeader(bis);
		skipRoutingHeader(bb);
		return routingHeader;
	}
	
	/**
	 * Skip the bytes of the routing header
	 * @param bb
	 */
	public static void skipRoutingHeader(final ByteBuffer bb) {
		bb.get(); 		// Routed or direct
		bb.getShort(); 	// Hop
		bb.get(); 		// Unused
		final short routingListLength = bb.getShort();	// Routing list length
		bb.position(bb.position() + routingListLength);
	}
	
	/**
	 * Decode a routed package header
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	protected static RoutingHeader decodeRoutedPackage(final InputStream inputStream) throws IOException {
		
		// Hop
		final byte[] hopBuffer = new byte[2];
		NetworkHelper.readExactlyBytes(inputStream, hopBuffer, 0, hopBuffer.length);
		final short hop = DataEncoderHelper.readShortFromByte(hopBuffer);
		
		// Skip one unused byte
		inputStream.skip(1);

		// Routing list list length
		final byte[] routingListLengthBuffer = new byte[2];
		NetworkHelper.readExactlyBytes(inputStream, routingListLengthBuffer, 0, routingListLengthBuffer.length);
		final short routingListLength = DataEncoderHelper.readShortFromByte(routingListLengthBuffer);

		final byte[] routingListBuffer = new byte[routingListLength];
		NetworkHelper.readExactlyBytes(inputStream, routingListBuffer, 0, routingListBuffer.length);
		final String routingList = new String(routingListBuffer);
		
		return new RoutingHeader(true, hop, routingList);
	}

	/**
	 * Decode a direct package header
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	protected static RoutingHeader decodeDirectPackage(final InputStream inputStream) throws IOException {
		// Skip 5 unused bytes
		NetworkHelper.skipBytesExcactly(inputStream, 5);
		return new RoutingHeader(false);
	}

	/**
	 * Encode the routing header into a byte buffer
	 * @throws IOException 
	 */
	public static byte[] encodeHeader(final RoutingHeader routingHeader) throws IOException {
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		
		if(routingHeader.isRoutedPackage()) {
			byteArrayOutputStream.write(RoutingHeader.ROUTED_PACKAGE);
			
			// Hop
			final ByteBuffer hop = DataEncoderHelper.shortToByteBuffer(routingHeader.getHop());
			byteArrayOutputStream.write(hop.array());
			
			// Unused
			byteArrayOutputStream.write(0x00);
			
			// Length of routing list
			final String routingList = routingHeader.getRoutingListAsString();
			final ByteBuffer rountingListLength = DataEncoderHelper.shortToByteBuffer((short) routingList.length());
			byteArrayOutputStream.write(rountingListLength.array());
			
			// Host list
			byteArrayOutputStream.write(routingList.getBytes());
			
		} else {
			byteArrayOutputStream.write(RoutingHeader.DIRECT_PACKAGE);
			
			// Hop
			byteArrayOutputStream.write(0x00);
			byteArrayOutputStream.write(0x00);
			
			// Unused
			byteArrayOutputStream.write(0x00);
			
			// Length of routing list
			byteArrayOutputStream.write(0x00);
			byteArrayOutputStream.write(0x00);
		}
		
		return byteArrayOutputStream.toByteArray();
	}
}