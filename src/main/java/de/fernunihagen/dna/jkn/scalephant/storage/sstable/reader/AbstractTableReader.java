package de.fernunihagen.dna.jkn.scalephant.storage.sstable.reader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fernunihagen.dna.jkn.scalephant.ScalephantService;
import de.fernunihagen.dna.jkn.scalephant.storage.StorageManagerException;
import de.fernunihagen.dna.jkn.scalephant.storage.sstable.SSTableConst;

public abstract class AbstractTableReader implements ScalephantService {

	/**
	 * The number of the table
	 */
	protected final int tablebumber;
	
	/**
	 * The name of the table
	 */
	protected final String name;
	
	/**
	 * The filename of the table
	 */
	protected File file;
	
	/**
	 * The Directory for the SSTables
	 */
	protected final String directory;
	
	/**
	 * The memory region
	 */
	protected MappedByteBuffer memory;

	/**
	 * The corresponding fileChanel
	 */
	protected FileChannel fileChannel;
	
	/**
	 * The Logger
	 */
	protected static final Logger logger = LoggerFactory.getLogger(AbstractTableReader.class);
	
	
	public AbstractTableReader(final String directory, final String relation, final int tablenumer) throws StorageManagerException {
		this.name = relation;
		this.directory = directory;
		this.tablebumber = tablenumer;

		this.file = constructFileToRead();
	}
	
	/**
	 * Construct the filename to read
	 * 
	 * @return
	 */
	protected abstract File constructFileToRead();
	
	/**
	 * Get the sequence number of the SSTable
	 * 
	 * @return
	 */
	public int getTablebumber() {
		return tablebumber;
	}


	/**
	 * Open a stored SSTable and read the magic bytes
	 * 
	 * @return a InputStream or null
	 * @throws StorageManagerException
	 */
	protected void validateFile() throws StorageManagerException {
		
		// Validate file - read the magic from the beginning
		final byte[] magicBytes = new byte[SSTableConst.MAGIC_BYTES.length];
		memory.get(magicBytes, 0, SSTableConst.MAGIC_BYTES.length);

		if(! Arrays.equals(magicBytes, SSTableConst.MAGIC_BYTES)) {
			throw new StorageManagerException("File " + file + " does not contain the magic bytes");
		}
	}
	
	/**
	 * Reset the position to the first element
	 */
	protected void resetPosition() {
		memory.position(SSTableConst.MAGIC_BYTES.length);
	}

	/**
	 * Init the resources
	 * 
	 * The file channel resource is closed in the shutdown method
	 */
	@SuppressWarnings("resource")
	@Override
	public void init() {
		try {
			fileChannel = new RandomAccessFile(file, "r").getChannel();
			memory = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
			memory.order(SSTableConst.SSTABLE_BYTE_ORDER);
			validateFile();
		} catch (Exception e) {
			if(! Thread.currentThread().isInterrupted()) {
				logger.error("Error during an IO operation", e);
			}
			shutdown();
		} 
	}

	@Override
	public void shutdown() {
		
		memory = null;
		
		if(fileChannel != null) {
			try {
				fileChannel.close();
				fileChannel = null;
			} catch (IOException e) {
				if(! Thread.currentThread().isInterrupted()) {
					logger.error("Error during an IO operation", e);
				}
			}
		}
	}
	
	/**
	 * Is the reader ready?
	 */
	protected boolean isReady() {
		return memory != null;
	}

	/**
	 * Get the name
	 * @return the file handle
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the file handle
	 * @return
	 */
	public File getFile() {
		return file;
	}

	/**
	 * Get the directory
	 * @return
	 */
	public String getDirectory() {
		return directory;
	}

	/**
	 * Delete the file
	 */
	public void delete() {
		shutdown();

		synchronized (this) {
			if(file != null) {
				logger.info("Delete file: " + file);
				file.delete();
				file = null;
			}
		}
	}
}
