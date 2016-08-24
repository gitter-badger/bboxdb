package de.fernunihagen.dna.scalephant.distribution.regionsplit;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fernunihagen.dna.scalephant.ScalephantConfiguration;
import de.fernunihagen.dna.scalephant.ScalephantConfigurationManager;
import de.fernunihagen.dna.scalephant.distribution.DistributionRegion;
import de.fernunihagen.dna.scalephant.distribution.DistributionRegionHelper;
import de.fernunihagen.dna.scalephant.distribution.membership.DistributedInstance;
import de.fernunihagen.dna.scalephant.distribution.membership.DistributedInstanceManager;
import de.fernunihagen.dna.scalephant.distribution.placement.ResourceAllocationException;
import de.fernunihagen.dna.scalephant.distribution.zookeeper.ZookeeperClient;
import de.fernunihagen.dna.scalephant.distribution.zookeeper.ZookeeperClientFactory;
import de.fernunihagen.dna.scalephant.distribution.zookeeper.ZookeeperException;
import de.fernunihagen.dna.scalephant.storage.entity.FloatInterval;

public class SimpleSplitStrategy extends RegionSplitStrategy {
	
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
		final List<DistributedInstance> systems = distributedInstanceManager.getInstances();
		
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

		try {
			// Allocate systems 
			final ZookeeperClient zookeeperClient = ZookeeperClientFactory.getZookeeperClient();
			DistributionRegionHelper.allocateSystemsToNewRegion(region.getLeftChild(), zookeeperClient);
			DistributionRegionHelper.allocateSystemsToNewRegion(region.getRightChild(), zookeeperClient);
		
			// Let the data settle down
			Thread.sleep(5000);
			
		} catch (ZookeeperException e) {
			logger.warn("Unable to assign systems to splitted region: " + region, e);
		} catch (ResourceAllocationException e) {
			logger.warn("Unable to find systems for splitted region: " + region, e);
		} catch (InterruptedException e) {
			logger.warn("Got InterruptedException while wait for settle down: " + region, e);
		}
		
	}
}