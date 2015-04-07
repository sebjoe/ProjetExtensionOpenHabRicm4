package org.openhab.binding.upnp.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UPnPActivator implements BundleActivator{

	private static Logger logger = LoggerFactory.getLogger(UPnPActivator.class);
	
	@Override
	public void start(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		logger.debug("UPnP binding has been started.");
		
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		logger.debug("UPnP binding has been stopped.");
		
	}

}
