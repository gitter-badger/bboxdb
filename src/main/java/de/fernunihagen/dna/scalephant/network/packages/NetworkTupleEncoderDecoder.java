package de.fernunihagen.dna.scalephant.network.packages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import de.fernunihagen.dna.scalephant.Const;
import de.fernunihagen.dna.scalephant.storage.entity.BoundingBox;
import de.fernunihagen.dna.scalephant.storage.entity.Tuple;
import de.fernunihagen.dna.scalephant.storage.entity.TupleAndTable;

public class NetworkTupleEncoderDecoder {
	
	/**
	 * Convert a ByteBuffer into a TupleAndTable object
	 * @param encodedPackage
	 * @return
	 */
	public static TupleAndTable decode(final ByteBuffer encodedPackage) {
		final short tableLength = encodedPackage.getShort();
		final short keyLength = encodedPackage.getShort();
		final int bBoxLength = encodedPackage.getInt();
		final int dataLength = encodedPackage.getInt();
		final long timestamp = encodedPackage.getLong();
		
		final byte[] tableBytes = new byte[tableLength];
		encodedPackage.get(tableBytes, 0, tableBytes.length);
		final String table = new String(tableBytes);
		
		final byte[] keyBytes = new byte[keyLength];
		encodedPackage.get(keyBytes, 0, keyBytes.length);
		final String key = new String(keyBytes);
		
		final byte[] boxBytes = new byte[bBoxLength];
		encodedPackage.get(boxBytes, 0, boxBytes.length);

		final byte[] dataBytes = new byte[dataLength];
		encodedPackage.get(dataBytes, 0, dataBytes.length);
		
		final BoundingBox boundingBox = BoundingBox.fromByteArray(boxBytes);
		
		final Tuple tuple = new Tuple(key, boundingBox, dataBytes, timestamp);
		
		return new TupleAndTable(tuple, table);
	}
	
	/**
	 * Write the tuple and the table onto a ByteArrayOutputStream
	 * @param bos
	 * @param routingHeader 
	 * @param tuple
	 * @param table
	 * @return 
	 * @throws IOException
	 */
	public static byte[] encode(final Tuple tuple, final String table) throws IOException {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		final byte[] tableBytes = table.getBytes();
		final byte[] keyBytes = tuple.getKey().getBytes();
		final byte[] bboxBytes = tuple.getBoundingBoxBytes();
		
		final ByteBuffer bb = ByteBuffer.allocate(20);
		bb.order(Const.APPLICATION_BYTE_ORDER);
		bb.putShort((short) tableBytes.length);
		bb.putShort((short) keyBytes.length);
		bb.putInt(bboxBytes.length);
		bb.putInt(tuple.getDataBytes().length);
		bb.putLong(tuple.getTimestamp());

		// Write body
		bos.write(bb.array());
		bos.write(tableBytes);
		bos.write(keyBytes);
		bos.write(bboxBytes);
		bos.write(tuple.getDataBytes());
		
		bos.close();
		
		return bos.toByteArray();
	}

}