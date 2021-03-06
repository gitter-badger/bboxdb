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
package org.bboxdb.distribution;

import java.util.HashMap;
import java.util.Map;

public class RegionIdMapperInstanceManager {
	
	/**
	 * The local mappings for a distribution group
	 */
	protected final static Map<DistributionGroupName, RegionIdMapper> instances;
	
	static {
		instances = new HashMap<DistributionGroupName, RegionIdMapper>();
	}
	
	/**
	 * Get the instance 
	 * @param distributionGroupName
	 */
	public static synchronized RegionIdMapper getInstance(final DistributionGroupName distributionGroupName) {
		if(! instances.containsKey(distributionGroupName)) {
			instances.put(distributionGroupName, new RegionIdMapper());
		}
		
		return instances.get(distributionGroupName);
	}
}
