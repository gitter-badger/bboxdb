/*******************************************************************************
 *
 *    Copyright (C) 2015-2017 the BBoxDB project
 *  
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License. 
 *    
 *******************************************************************************/
package org.bboxdb.distribution.placement;

import java.util.ArrayList;
import java.util.List;

import org.bboxdb.distribution.membership.DistributedInstance;
import org.bboxdb.distribution.membership.event.DistributedInstanceState;
import org.junit.Assert;
import org.junit.Test;

public class TestRoundRobinRessourcePlacement extends TestRandomRessourcePlacement {
	
	/**
	 * Get the placement strategy for the test
	 * @return
	 */
	@Override
	public ResourcePlacementStrategy getPlacementStrategy() {
		return new RoundRobinResourcePlacementStrategy();
	}

	/**
	 * Test round robin placement 1
	 * @throws ResourceAllocationException
	 */
	@Test
	public void testRoundRobinPlacement1() throws ResourceAllocationException {
		final ResourcePlacementStrategy resourcePlacementStrategy = getPlacementStrategy();
		final List<DistributedInstance> systems = new ArrayList<DistributedInstance>();
		systems.add(new DistributedInstance("node1:123", "0.1", DistributedInstanceState.READY));
		systems.add(new DistributedInstance("node2:123", "0.1", DistributedInstanceState.READY));
		systems.add(new DistributedInstance("node3:123", "0.1", DistributedInstanceState.READY));
		systems.add(new DistributedInstance("node4:123", "0.1", DistributedInstanceState.READY));
		
		Assert.assertEquals(systems.get(0), resourcePlacementStrategy.getInstancesForNewRessource(systems));
		Assert.assertEquals(systems.get(1), resourcePlacementStrategy.getInstancesForNewRessource(systems));
		Assert.assertEquals(systems.get(2), resourcePlacementStrategy.getInstancesForNewRessource(systems));
		Assert.assertEquals(systems.get(3), resourcePlacementStrategy.getInstancesForNewRessource(systems));
		Assert.assertEquals(systems.get(0), resourcePlacementStrategy.getInstancesForNewRessource(systems));
	}
	
	/**
	 * Test round robin placement 2 (removed system)
	 * @throws ResourceAllocationException
	 */
	@Test
	public void testRoundRobinPlacement2() throws ResourceAllocationException {
		final ResourcePlacementStrategy resourcePlacementStrategy = getPlacementStrategy();
		final List<DistributedInstance> systems = new ArrayList<DistributedInstance>();
		systems.add(new DistributedInstance("node1:123", "0.1", DistributedInstanceState.READY));
		systems.add(new DistributedInstance("node2:123", "0.1", DistributedInstanceState.READY));
		systems.add(new DistributedInstance("node3:123", "0.1", DistributedInstanceState.READY));
		systems.add(new DistributedInstance("node4:123", "0.1", DistributedInstanceState.READY));
		
		Assert.assertEquals(systems.get(0), resourcePlacementStrategy.getInstancesForNewRessource(systems));
		Assert.assertEquals(systems.get(1), resourcePlacementStrategy.getInstancesForNewRessource(systems));
		systems.remove(1);
		Assert.assertEquals(systems.get(0), resourcePlacementStrategy.getInstancesForNewRessource(systems));
	}
	

}
