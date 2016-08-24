package de.fernunihagen.dna.scalephant.storage.entity;

import de.fernunihagen.dna.scalephant.storage.sstable.SSTableConst;

public class DeletedTuple extends Tuple {

	public DeletedTuple(final String key) {
		super(key, null, null);
	}

	@Override
	public String toString() {
		return "DeletedTuple [key=" + key + ", timestamp=" + timestamp + "]";
	}
	
	@Override
	public byte[] getDataBytes() {
		return SSTableConst.DELETED_MARKER;
	}
	
	@Override
	public byte[] getBoundingBoxBytes() {
		return SSTableConst.DELETED_MARKER;
	}
}