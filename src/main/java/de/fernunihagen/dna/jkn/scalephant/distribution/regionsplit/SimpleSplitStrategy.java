package de.fernunihagen.dna.jkn.scalephant.distribution.regionsplit;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fernunihagen.dna.jkn.scalephant.ScalephantConfiguration;
import de.fernunihagen.dna.jkn.scalephant.ScalephantConfigurationManager;
import de.fernunihagen.dna.jkn.scalephant.distribution.DistributionRegion;
import de.fernunihagen.dna.jkn.scalephant.distribution.ZookeeperClient;
import de.fernunihagen.dna.jkn.scalephant.distribution.ZookeeperClientFactory;
import de.fernunihagen.dna.jkn.scalephant.distribution.ZookeeperException;
import de.fernunihagen.dna.jkn.scalephant.distribution.membership.DistributedInstance;
import de.fernunihagen.dna.jkn.scalephant.distribution.membership.DistributedInstanceManager;
import de.fernunihagen.dna.jkn.scalephant.distribution.resource.RandomResourcePlacementStrategy;
import de.fernunihagen.dna.jkn.scalephant.storage.entity.FloatInterval;

public class SimpleSplitStrategy extends RegionSplitter {
	
	/**
	 * The Logger
	 */
	protected final static Logger logger = LoggerFactory.getLogger(SimpleSplitStrategy.class);

	/**
	 * Test if a split is needed
	 */
	@Override
	public boolean isSplitNeeded(final int totalTuplesInTable) {
		final ScalephantConfiguration configuration = ScalephantConfigurationManager.getConfiguration();
		final int maxEntries = configuration.getSstableMaxEntries();
		
		if(totalTuplesInTable > maxEntries) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Perform a split of the given distribution region
	 */
	@Override
	protected void performSplit(final DistributionRegion region) {
		
		final DistributedInstanceManager distributedInstanceManager = DistributedInstanceManager.getInstance();
		final Set<DistributedInstance> systems = distributedInstanceManager.getInstances();
		
		if(systems.isEmpty()) {
			logger.warn("Unable to split region, no ressources are avilable: " + region);
			return;
		}
		
		logger.info("Performing split of region: " + region);
		
		// Split region
		final int splitDimension = region.getSplitDimension();
		final FloatInterval interval = region.getConveringBox().getIntervalForDimension(splitDimension);
		
		logger.info("Split at dimension:" + splitDimension + " interval: " + interval);
		float midpoint = interval.getMidpoint();
		
		logger.info("Set split at:" + midpoint);
		region.setSplit(midpoint);
		
		// Assign systems
		final RandomResourcePlacementStrategy resourcePlacementStrategy = new RandomResourcePlacementStrategy();
		
		final DistributedInstance systemLeftChild = resourcePlacementStrategy.findSystemToAllocate(systems);
		final DistributedInstance systemRightChild = resourcePlacementStrategy.findSystemToAllocate(systems);
		
		try {
			final ZookeeperClient zookeeperClient = ZookeeperClientFactory.getZookeeperClient();
			zookeeperClient.addSystemToDistributionRegion(region.getLeftChild(), systemLeftChild);
			zookeeperClient.addSystemToDistributionRegion(region.getRightChild(), systemRightChild);
		} catch (ZookeeperException e) {
			logger.warn("Unable to assign systems to splitted region: " + region, e);
		}
		
	}
}