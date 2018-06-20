/*
 * Copyright (C) 2016
 *
 * Author: Jody Marca <jmarca@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.bmwcarit.barefoot.road;

import com.bmwcarit.barefoot.road.BaseRoad;
import com.bmwcarit.barefoot.road.Heading;
import com.esri.core.geometry.Polyline;

/**
 * RoadOutputPath data structure for a openpenstreetmap road segment.
 *
 * Provade a geometry (subset or equals to openpenstreetmap geometry) and 
 * a link to {@link BaseRoad} with relative {@link Heading}
 */
public class RoadOutputPath {

	private BaseRoad base = null;
	private Polyline geometry = null;
	private Heading heading = null;
	
	/**
	 * Constructs {@link RoadOutputPath} object.
	 * 
	 * @param base {@link BaseRoad} object associated
	 * @param geometry It's a part of Road geometry as {@link Polyline} object.
	 * @param heading {@link Heading} needed for speed computation
	 */
	public RoadOutputPath(BaseRoad base, Polyline geometry, Heading heading) {
		this.base = base;
		this.geometry = geometry;
		this.heading = heading;
	}
		
	/**
	 * Gets {@link BaseRoad} object associated
	 * @return baseRoad object associated
	 */
	public BaseRoad getBase() {
		return base;
	}
	
	/**
     * Gets road's geometry as a {@link Polyline} as part of Road geometry.
     *
     * @return Road's geometry as {@link Polyline} as part of Road geometry.
     */
	public Polyline getGeometry() {
		return geometry;
	}

	/**
	 * Gets the {@link BaseRoad} identifier if presents
	 * 
	 * @return baseroad identifier or null 
	 */
	public Long getId(){
		if(base == null){
			return null;
		}else{
			return base.refid();
		}
	}

	/**
	 * Gets {@link Heading} needed for speed computation
	 * @return heading associated to the instance
	 */
	public Heading getHeading() {
		return heading;
	}
	

}
