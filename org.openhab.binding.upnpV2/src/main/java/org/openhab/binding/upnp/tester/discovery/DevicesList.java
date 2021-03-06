/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.openhab.binding.upnp.tester.discovery;

import java.util.ArrayList;
import java.util.Iterator;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class DevicesList {
	
	ArrayList devices;	
	DeviceChangeListener listener = null;
	
	public DevicesList(){
		devices = new ArrayList();
	}
	
	
	public boolean add(DeviceNode dev) {
		if(listener!=null) 
			listener.addedDevice(dev);
		return devices.add(dev);
	}

	public Iterator iterator() {
		return devices.iterator();
	}

	public boolean remove(DeviceNode dev) {
		if(listener!=null)
			listener.removedDevice(dev);
		return devices.remove(dev);
	}

	public DeviceChangeListener getChangeListener() {
		return listener;
	}

	public void setChangeListener(DeviceChangeListener listener) {
		this.listener = listener;
	}
	
	
}
