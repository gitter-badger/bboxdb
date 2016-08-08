package de.fernunihagen.dna.jkn.scalephant;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.fernunihagen.dna.jkn.scalephant.distribution.membership.DistributedInstance;
import de.fernunihagen.dna.jkn.scalephant.distribution.placement.ResourceAllocationException;
import de.fernunihagen.dna.jkn.scalephant.distribution.placement.ResourcePlacementStrategy;
import de.fernunihagen.dna.jkn.scalephant.distribution.placement.RoundRobinResourcePlacementStrategy;

public class TestResoucePlacement {

	/**
	 * Test round robin placement 1 (empty list)
	 * @throws ResourceAllocationException
	 */
	@Test(expected=ResourceAllocationException.class)
	public void testRoundRobinPlacement0() throws ResourceAllocationException {
		final ResourcePlacementStrategy resourcePlacementStrategy = new RoundRobinResourcePlacementStrategy();
		final List<DistributedInstance> systems = new ArrayList<DistributedInstance>();
		resourcePlacementStrategy.getInstancesForNewRessource(systems);
	}
	
	/**
	 * Test round robin placement 2
	 * @throws ResourceAllocationException
	 */
	@Test
	public void testRoundRobinPlacement2() throws ResourceAllocationException {
		final ResourcePlacementStrategy resourcePlacementStrategy = new RoundRobinResourcePlacementStrategy();
		final List<DistributedInstance> systems = new ArrayList<DistributedInstance>();
		systems.add(new DistributedInstance("node1:123"));
		systems.add(new DistributedInstance("node2:123"));
		systems.add(new DistributedInstance("node3:123"));
		systems.add(new DistributedInstance("node4:123"));
		
		Assert.assertEquals(systems.get(0), resourcePlacementStrategy.getInstancesForNewRessource(systems));
		Assert.assertEquals(systems.get(1), resourcePlacementStrategy.getInstancesForNewRessource(systems));
		Assert.assertEquals(systems.get(2), resourcePlacementStrategy.getInstancesForNewRessource(systems));
		Assert.assertEquals(systems.get(3), resourcePlacementStrategy.getInstancesForNewRessource(systems));
		Assert.assertEquals(systems.get(0), resourcePlacementStrategy.getInstancesForNewRessource(systems));
	}
	
	/**
	 * Test round robin placement 3 (all systems are blacklisted)
	 * @throws ResourceAllocationException
	 */
	@Test(expected=ResourceAllocationException.class)
	public void testRoundRobinPlacement3() throws ResourceAllocationException {
		final ResourcePlacementStrategy resourcePlacementStrategy = new RoundRobinResourcePlacementStrategy();
		final List<DistributedInstance> systems = new ArrayList<DistributedInstance>();
		systems.add(new DistributedInstance("node1:123"));
		systems.add(new DistributedInstance("node2:123"));
		systems.add(new DistributedInstance("node3:123"));
		systems.add(new DistributedInstance("node4:123"));
		
		Assert.assertEquals(systems.get(0), resourcePlacementStrategy.getInstancesForNewRessource(systems, systems));
	}
	
	/**
	 * Test round robin placement 4 (removed system)
	 * @throws ResourceAllocationException
	 */
	@Test
	public void testRoundRobinPlacement4() throws ResourceAllocationException {
		final ResourcePlacementStrategy resourcePlacementStrategy = new RoundRobinResourcePlacementStrategy();
		final List<DistributedInstance> systems = new ArrayList<DistributedInstance>();
		systems.add(new DistributedInstance("node1:123"));
		systems.add(new DistributedInstance("node2:123"));
		systems.add(new DistributedInstance("node3:123"));
		systems.add(new DistributedInstance("node4:123"));
		
		Assert.assertEquals(systems.get(0), resourcePlacementStrategy.getInstancesForNewRessource(systems));
		Assert.assertEquals(systems.get(1), resourcePlacementStrategy.getInstancesForNewRessource(systems));
		systems.remove(1);
		Assert.assertEquals(systems.get(0), resourcePlacementStrategy.getInstancesForNewRessource(systems));
	}

}
