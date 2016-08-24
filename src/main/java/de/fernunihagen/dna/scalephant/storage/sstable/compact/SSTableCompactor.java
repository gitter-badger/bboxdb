package de.fernunihagen.dna.scalephant.storage.sstable.compact;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fernunihagen.dna.scalephant.storage.StorageManagerException;
import de.fernunihagen.dna.scalephant.storage.entity.DeletedTuple;
import de.fernunihagen.dna.scalephant.storage.entity.Tuple;
import de.fernunihagen.dna.scalephant.storage.sstable.SSTableWriter;
import de.fernunihagen.dna.scalephant.storage.sstable.reader.SSTableKeyIndexReader;

public class SSTableCompactor {

	/**
	 * The list of sstables to compact
	 */
	protected final List<SSTableKeyIndexReader> sstableIndexReader;
	
	/**
	 * Our output sstable writer
	 */
	protected final SSTableWriter sstableWriter;
	
	/**
	 * Major or minor compaction? In a major compaction, the deleted tuple
	 * marker can be removed.
	 */
	protected boolean majorCompaction = false;
	
	/**
	 * The amount of read tuples
	 */
	protected int readTuples;
	
	/**
	 * The amount of written tuples
	 */
	protected int writtenTuples;

	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(SSTableCompactor.class);

	public SSTableCompactor(final List<SSTableKeyIndexReader> sstableIndexReader, 
			final SSTableWriter sstableWriter) {
		
		super();
		this.sstableIndexReader = sstableIndexReader;
		this.sstableWriter = sstableWriter;
		this.readTuples = 0;
		this.writtenTuples = 0;
	}
	
	/** 
	 * Execute the compactation of the input sstables
	 * 
	 * @return success or failure
	 */
	public boolean executeCompactation() {
		
		final List<Iterator<Tuple>> iterators = new ArrayList<Iterator<Tuple>>(sstableIndexReader.size());
		final List<Tuple> tuples = new ArrayList<Tuple>(sstableIndexReader.size());
		
		// Open iterators for input sstables
		for(final SSTableKeyIndexReader reader : sstableIndexReader) {
			iterators.add(reader.iterator());
			tuples.add(null);
		}
		
		try {
			sstableWriter.open();
			logger.info("Execute a new compactation into file " + sstableWriter.getSstableFile());

			boolean done = false;
			
		    while(done == false) {
				
				done = refreshTuple(iterators, tuples);
				
				final Tuple tuple = getTupleWithTheLowestKey(iterators, tuples);
				
				// Write the tuple
				if(tuple != null) {
					consumeTuplesForKey(tuples, tuple.getKey());
					
					// Don't add deleted tuples to output in a major compaction
					if(! (isMajorCompaction() && (tuple instanceof DeletedTuple))) {
						sstableWriter.addNextTuple(tuple);
						writtenTuples++;
					}
				}
			}
			
			sstableWriter.close();
		} catch (StorageManagerException e) {
			logger.error("Exception while compatation", e);
			return false;
		}
		
		return true;
	}

	/**
	 * Consume all tuples for key
	 * @param tuples
	 * @param key
	 */
	protected void consumeTuplesForKey(final List<Tuple> tuples, String key) {
		// Consume the key
		for(int i = 0; i < tuples.size(); i++) {
			final Tuple nextTuple = tuples.get(i);
			
			if(nextTuple == null) {
				continue;
			}
			
			if(key.equals(nextTuple.getKey())) {
				tuples.set(i, null);
			}
		}
	}

	/**
	 * Determine the tuple with the lowest key
	 * @param iterators
	 * @param tuples
	 * @return
	 */
	protected Tuple getTupleWithTheLowestKey(
			final List<Iterator<Tuple>> iterators, final List<Tuple> tuples) {
		// Get tuple with the lowest key
		Tuple tuple = null;				

		for(int i = 0; i < iterators.size(); i++) {
			
			final Tuple nextTuple = tuples.get(i);
			
			if(nextTuple == null) {
				continue;
			}
			
			if(tuple == null) {
				tuple = nextTuple;
				continue;
			}
			
			int result = tuple.compareTo(nextTuple);
			
			if(result > 0) {
				tuple = nextTuple;
			} 
		}
				
		return tuple;
	}

	/**
	 * Read a tuple from each iterator, if the corresponding position 
	 * of out buffer
	 * 
	 * @param iterators
	 * @param tuples
	 * @return
	 */
	protected boolean refreshTuple(final List<Iterator<Tuple>> iterators,
			final List<Tuple> tuples) {
		
		boolean done = true;
		
		// Refresh Tuples
		for(int i = 0; i < iterators.size(); i++) {
			if(tuples.get(i) == null) {
				if(iterators.get(i).hasNext()) {
					tuples.set(i, iterators.get(i).next());
					readTuples++;
				}
			}
			
			// We have tuple to process
			if(done == true && tuples.get(i) != null) {
				done = false;
			}
		}
		return done;
	}
	
	/**
	 * Return the SSTable file
	 * @return
	 */
	public File getSstableFile() {
		return sstableWriter.getSstableFile();
	}
	
	/**
	 * Return the SSTable index file
	 * @return
	 */
	public File getSstableIndexFile() {
		return sstableWriter.getSstableIndexFile();
	}

	/**
	 * Is this a major compaction?
	 * @return
	 */
	public boolean isMajorCompaction() {
		return majorCompaction;
	}

	/**
	 * Set major compaction flag
	 * @param majorCompaction
	 */
	public void setMajorCompaction(boolean majorCompaction) {
		this.majorCompaction = majorCompaction;
	}
	
	/**
	 * Get the amount of read tuples
	 * @return
	 */
	public int getReadTuples() {
		return readTuples;
	}
	
	/**
	 * Get the amount of written tuples
	 * @return
	 */
	public int getWrittenTuples() {
		return writtenTuples;
	}
	
}