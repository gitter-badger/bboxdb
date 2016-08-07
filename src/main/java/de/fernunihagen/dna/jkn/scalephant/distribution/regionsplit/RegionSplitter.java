package de.fernunihagen.dna.jkn.scalephant.distribution.regionsplit;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fernunihagen.dna.jkn.scalephant.distribution.DistributionGroupCache;
import de.fernunihagen.dna.jkn.scalephant.distribution.DistributionRegion;
import de.fernunihagen.dna.jkn.scalephant.distribution.ZookeeperClient;
import de.fernunihagen.dna.jkn.scalephant.distribution.ZookeeperClientFactory;
import de.fernunihagen.dna.jkn.scalephant.distribution.ZookeeperException;
import de.fernunihagen.dna.jkn.scalephant.distribution.membership.DistributedInstance;
import de.fernunihagen.dna.jkn.scalephant.distribution.membership.MembershipConnectionService;
import de.fernunihagen.dna.jkn.scalephant.network.client.ScalephantClient;
import de.fernunihagen.dna.jkn.scalephant.storage.StorageInterface;
import de.fernunihagen.dna.jkn.scalephant.storage.StorageManager;
import de.fernunihagen.dna.jkn.scalephant.storage.StorageManagerException;
import de.fernunihagen.dna.jkn.scalephant.storage.entity.SSTableName;
import de.fernunihagen.dna.jkn.scalephant.storage.entity.Tuple;
import de.fernunihagen.dna.jkn.scalephant.storage.sstable.SSTableManager;
import de.fernunihagen.dna.jkn.scalephant.storage.sstable.reader.SSTableFacade;
import de.fernunihagen.dna.jkn.scalephant.storage.sstable.reader.SSTableKeyIndexReader;

public abstract class RegionSplitter {
	
	/**
	 * The Logger
	 */
	protected final static Logger logger = LoggerFactory.getLogger(RegionSplitter.class);

	/**
	 * Is a split needed?
	 * @param totalTuplesInTable
	 * @return
	 */
	public abstract boolean isSplitNeeded(final int totalTuplesInTable);
	
	/**
	 * Perform a split of the given region
	 * @param region
	 */
	protected abstract void performSplit(final DistributionRegion region);

	/**
	 * Perform a SSTable split
	 * @param ssTableName
	 * @return Split performed or not
	 */
	public boolean performSplit(final SSTableName ssTableName) {
		logger.info("Performing split for: " + ssTableName);
		
		if(! ssTableName.isNameprefixValid()) {
			logger.warn("Unable to split table, nameprefix is invalid: " + ssTableName);
			return false;
		}
		
		try {
			final ZookeeperClient zookeeperClient = ZookeeperClientFactory.getZookeeperClient();
			
			final DistributionRegion distributionGroup = DistributionGroupCache.getGroupForGroupName(ssTableName.getDistributionGroup(), zookeeperClient);
			
			final DistributionRegion region = distributionGroup.getDistributionRegionForNamePrefix(ssTableName.getNameprefix());
			
			if(region == null) {
				logger.warn("Unable to find nameprefix in distributionGroup: " + ssTableName);
				return false;
			}
			
			if(! region.isLeafRegion()) {
				logger.error("Region is not a leaf region, unable to split:" + region);
				return false;
			}
			
			performSplit(region);
			redistributeData(region);
		} catch (ZookeeperException e) {
			logger.warn("Unable to split sstable: " + ssTableName, e);
			return false;
		}
		
		return true;
	}

	/**
	 * Redistribute data after region split
	 * @param region
	 */
	protected void redistributeData(final DistributionRegion region) {
		logger.info("Redistributing data for region: " + region);
		
		final List<SSTableName> localTables = StorageInterface.getAllTables();
		for(final SSTableName ssTableName : localTables) {
			if(ssTableName.getDistributionGroupObject().equals(region.getDistributionGroupName())) {
				redistributeTable(region, ssTableName);
			}
		}
		
		logger.info("Redistributing data for region: " + region + " DONE");
	}

	/**
	 * Redistribute the given sstable
	 * @param region
	 * @param ssTableName
	 */
	protected void redistributeTable(final DistributionRegion region, final SSTableName ssTableName) {
		logger.info("Redistributing table " + ssTableName.getFullname());
		
		try {
			// FIXME: Spread in memory data
			final StorageManager storageManager = StorageInterface.getStorageManager(ssTableName);
			final SSTableManager ssTableManager = storageManager.getSstableManager();
			final List<SSTableFacade> facades = ssTableManager.getSstableFacades();

			spreadFacades(region, ssTableName, facades);
			
		} catch (StorageManagerException e) {
			logger.warn("Got an exception while distributing tuples for: " + ssTableName, e);
		}
		
		logger.info("Redistributing table " + ssTableName.getFullname() + " is DONE");
	}

	/**
	 * Spread the given sstable facades
	 * @param region
	 * @param ssTableName
	 * @param facades
	 */
	protected void spreadFacades(final DistributionRegion region, final SSTableName ssTableName, final List<SSTableFacade> facades) {
		final DistributionRegion leftRegion = region.getLeftChild();
		final DistributionRegion rightRegion = region.getRightChild();
		
		for(final SSTableFacade facade: facades) {
			final boolean acquired = facade.acquire();
			if(acquired) {
				
				logger.info("Spread sstable facade: " + facade.getName());
				
				final SSTableKeyIndexReader reader = facade.getSsTableKeyIndexReader();
				for(final Tuple tuple : reader) {
					
					// Spread to the left region
					if(tuple.getBoundingBox().overlaps(leftRegion.getConveringBox())) {
						final Collection<DistributedInstance> instances = leftRegion.getSystems();
						spreadTupleToSystems(ssTableName, tuple, instances);
					}
					
					// Spread to the right region
					if(tuple.getBoundingBox().overlaps(rightRegion.getConveringBox())) {
						final Collection<DistributedInstance> instances = rightRegion.getSystems();
						spreadTupleToSystems(ssTableName, tuple, instances);							
					}
				}
				
				facade.release();
			}
		}
	}

	/**
	 * Spread the tuple to the given list of systems
	 * @param ssTableName
	 * @param tuple
	 * @param instances
	 * @return 
	 */
	protected boolean spreadTupleToSystems(final SSTableName ssTableName, final Tuple tuple, final Collection<DistributedInstance> instances) {
		final MembershipConnectionService membershipConnectionService = MembershipConnectionService.getInstance();
		
		for(final DistributedInstance instance : instances) {
			final ScalephantClient connection = membershipConnectionService.getConnectionForInstance(instance);
			
			if(connection == null) {
				logger.warn("Connection to system is not known, unable to distribute tuple: " + connection);
				return false;
			}
			
			connection.insertTuple(ssTableName.getFullname(), tuple);
		}
		
		return true;
	}
}