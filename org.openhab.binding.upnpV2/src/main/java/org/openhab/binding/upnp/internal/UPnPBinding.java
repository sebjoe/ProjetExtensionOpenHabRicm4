/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.upnp.internal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


import org.openhab.binding.upnp.UPnPBindingProvider;

import org.apache.commons.lang.StringUtils;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ungoverned.gravity.servicebinder.ServiceBinderContext;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.service.upnp.UPnPService;

//import org.ungoverned.gravity.servicebinder.ServiceBinderContext;
//import org.ungoverned.osgi.service.shell.Command;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
	

/**
 * Implement this class if you are going create an actively polling service
 * like querying a Website/Device.
 * 
 * @author Thibault_Saussac_and_Sebastien_Toussaint
 * @since 1.7.0
 */
public class UPnPBinding extends AbstractActiveBinding<UPnPBindingProvider> implements Command {
	
	/////////////////// MODIF
	private static final String SERVICE_CMD = "services";

	private static final String DEVICE_CMD = "devices";

	private static final String LISTENER_CMD = "listeners";

	private static final String HELP_CMD = "help";

	private BundleContext m_context = null;

	private List devices = new ArrayList();

	private List eventListeners = new ArrayList();

	private List deviceServiceReferences = new ArrayList();

	private List eventListenerServiceReferences = new ArrayList();
	
	
	// List of time servers: http://tf.nist.gov/service/time-servers.html
		protected String hostname = "ptbtime1.ptb.de";
	
	/////////////////// End Modif

	private static final Logger logger = 
		LoggerFactory.getLogger(UPnPBinding.class);

	/**
	 * The BundleContext. This is only valid when the bundle is ACTIVE. It is set in the activate()
	 * method and must not be accessed anymore once the deactivate() method was called or before activate()
	 * was called.
	 */
	private BundleContext bundleContext;

	
	/** 
	 * the refresh interval which is used to poll values from the UPnP
	 * server (optional, defaults to 60000ms)
	 */
	private long refreshInterval = 60000;
	
	
	public UPnPBinding(ServiceBinderContext serviceBinderContext) {
		m_context = serviceBinderContext.getBundleContext();
	}
		
	
	/**
	 * Called by the SCR to activate the component with its configuration read from CAS
	 * 
	 * @param bundleContext BundleContext of the Bundle that defines this component
	 * @param configuration Configuration properties for this component obtained from the ConfigAdmin service
	 */
	public void activate(final BundleContext bundleContext, final Map<String, Object> configuration) {
		this.bundleContext = bundleContext;

		// the configuration is guaranteed not to be null, because the component definition has the
		// configuration-policy set to require. If set to 'optional' then the configuration may be null
		
			
		// to override the default refresh interval one has to add a 
		// parameter to openhab.cfg like <bindingName>:refresh=<intervalInMs>
		String refreshIntervalString = (String) configuration.get("refresh");
		if (StringUtils.isNotBlank(refreshIntervalString)) {
			refreshInterval = Long.parseLong(refreshIntervalString);
		}

		// read further config parameters here ...

		setProperlyConfigured(true);
	}
	
	/**
	 * Called by the SCR when the configuration of a binding has been changed through the ConfigAdmin service.
	 * @param configuration Updated configuration properties
	 */
	public void modified(final Map<String, Object> configuration) {
		// update the internal configuration accordingly
	}
	
	/**
	 * Called by the SCR to deactivate the component when either the configuration is removed or
	 * mandatory references are no longer satisfied or the component has simply been stopped.
	 * @param reason Reason code for the deactivation:<br>
	 * <ul>
	 * <li> 0 – Unspecified
     * <li> 1 – The component was disabled
     * <li> 2 – A reference became unsatisfied
     * <li> 3 – A configuration was changed
     * <li> 4 – A configuration was deleted
     * <li> 5 – The component was disposed
     * <li> 6 – The bundle was stopped
     * </ul>
	 */
	public void deactivate(final int reason) {
		this.bundleContext = null;
		// deallocate resources here that are no longer needed and 
		// should be reset when activating this binding again
	}

	
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected String getName() {
		return "UPnP Refresh Service";
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void execute(String commandLine, PrintStream out, PrintStream err) {

		boolean hasFilterFlag = true;

		try {

			// Parse the commandLine to get the upnp command.
			StringTokenizer st = new StringTokenizer(commandLine);
			// Ignore the invoking command.
			st.nextToken();
			// Try to get the command, default is HELP command.
			String command = HELP_CMD;
			try {
				command = st.nextToken();
			} catch (Exception ex) {
				// Ignore.
			}

			// Perform the specified command.
			if ((command == null) || (command.equals(HELP_CMD))) {

				help(out, st);

			} else if (command.equals(SERVICE_CMD)
					|| command.equals(DEVICE_CMD)) { /*
													  * rajouter les nouvelles
													  * options
													  */

				Filter filter = null;
				int start = commandLine.indexOf("(");
				int end = commandLine.lastIndexOf(")");

				if ((start == -1) && (end == -1)) {

					hasFilterFlag = false;
				}

				if ((start > end) || ((start == -1) && (end > -1))) {
					err.println("bad filter for target");
					return;
				}

				String filterStr = "";
				if (hasFilterFlag) {
					filterStr = commandLine.substring(start, end + 1);

					try {
						filter = m_context.createFilter(filterStr);
					} catch (InvalidSyntaxException e) {
						err.println("bad filter syntax for target");
						return;
					}
				}

				if (command.equals(DEVICE_CMD)) {

					boolean printed = false;

					if (hasFilterFlag) {
						for (int i = 0; i < this.devices.size(); i++) {

							UPnPDevice dev = (UPnPDevice) this.devices
									.get(i);
							if (filter.match(dev.getDescriptions(null))) {

								String name = (String) dev
										.getDescriptions(null).get(
												UPnPDevice.FRIENDLY_NAME);
								String autor = (String) dev.getDescriptions(
										null).get(UPnPDevice.MANUFACTURER);

								if (name != null) {

									if (!printed) {
										printed = true;
										out.println("");
									}
									out.print(name + " (FrendlyName) \t");
									if (autor != null) {
										out.print(autor + " (manufacturer)");
									}
									out.println("");
								} else {
									out
											.print(" Existing device but no name available ! ");
								}
							}

						}//end boucle for

						if (printed) {
							out.println("");
						} else {
							out
									.println("No matching UPnP device with your selection.");
						}

					} else { //no filter case

						for (int i = 0; i < this.devices.size(); i++) {

							UPnPDevice dev = (UPnPDevice) this.devices
									.get(i);

							String name = (String) dev.getDescriptions(null)
									.get(UPnPDevice.FRIENDLY_NAME);
							String autor = (String) dev.getDescriptions(null)
									.get(UPnPDevice.MANUFACTURER);

							if (name != null) {

								if (!printed) {
									printed = true;
									out.println("");
								}
								out.print(name + " (FrendlyName) \t");
								if (autor != null) {
									out.print(autor + "(manufacturer)");
								}
								out.println("");
							} else {
								out
										.print(" Existing device but no name available ! ");
							}

						}

						if (printed) {
							out.println("");
						} else {
							out.println("No UPnP device available ");
						}

					}

				}// end if (command.equals(DEVICE_CMD))

				else if (command.equals(SERVICE_CMD)) {

					boolean printed = false;

					if (hasFilterFlag) { //si il y a un filtre ben on fait le
						// filtrage dans le parcours
						for (int i = 0; i < this.devices.size(); i++) {

							UPnPDevice dev = (UPnPDevice) this.devices
									.get(i);
							UPnPService[] services = (UPnPService[]) dev
									.getServices();
							boolean trouve = false;
							StringBuffer afficheService = new StringBuffer(
									"\tServices provided : \n");

							for (int j = 0; j < services.length; j++) {

								Dictionary dictionary = setDic(services[j]);
								afficheService.append("\t - ").append(
										services[j].getType()).append('\n');
								//out.println("*** DEBUG : filter =
								// \n"+filter.toString());
								//out.println("*** DEBUG : dictionary =
								// \n"+dictionary.toString());
								if (filter.match(dictionary)) {
									trouve = true;
								}
							}

							if (trouve) {

								String name = (String) dev
										.getDescriptions(null).get(
												UPnPDevice.FRIENDLY_NAME);
								String autor = (String) dev.getDescriptions(
										null).get(UPnPDevice.MANUFACTURER);

								if (name != null) {

									if (!printed) {
										printed = true;
										out.println("");
									}
									out.print(name + " (FrendlyName) \t");
									if (autor != null) {
										out.print(autor + " (manufacturer)");
									}
									out.println("");
									out.print(afficheService.toString());
								} else {
									out
											.print(" Existing device but no name available ! ");
								}
								out.println("");
							}

						}//fin boucle for

						if (printed) {
							out.println("");
						} else {
							out
									.println("No matching UPnP device with your selection .");
						}

					} else { //si y a pas de filtre et ben pas de filtrage

						for (int i = 0; i < this.devices.size(); i++) {

							UPnPDevice dev = (UPnPDevice) this.devices
									.get(i);
							UPnPService[] services = (UPnPService[]) dev
									.getServices();

							String afficheService = "\tServices provided : \n";

							for (int j = 0; j < services.length; j++) {

								Dictionary dictionary = setDic(services[j]);
								afficheService = afficheService + "\t - "
										+ services[j].getType() + "\n";

							}

							String name = (String) dev.getDescriptions(null)
									.get(UPnPDevice.FRIENDLY_NAME);
							String autor = (String) dev.getDescriptions(null)
									.get(UPnPDevice.MANUFACTURER);

							if (name != null) {

								if (!printed) {
									printed = true;
									out.println("");
								}
								out.print(name + " (FrendlyName) \t");
								if (autor != null) {
									out.print(autor + " (manufacturer)");
								}
								out.println("");
								out.print(afficheService);
							} else {
								out
										.print(" Existing device but no name available ! ");
							}
							out.println("");

						}//fin boucle for
						if (printed) {
							out.println("");
						} else {
							out.println("No UPnP device available ");
						}

					}

				}// end if (command.equals(SERVICE_CMD))

			} else if (command.equals(LISTENER_CMD)) {
				err.println("Not implemented " + command);
			} else {
				err.println("Unknown upnp " + command
						+ " -- show 'upnp help' for upnp syntax information");
			}

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		// the code being executed when a command was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveCommand({},{}) is called!", itemName, command);
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		// the code being executed when a state was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveUpdate({},{}) is called!", itemName, newState);
	}

	/////////////// MOdif
	@Override
	public String format(String pattern) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	public UPnPCommandImpl(ServiceBinderContext serviceBinderContext) {
		m_context = serviceBinderContext.getBundleContext();
	}*/

	/**
	 * for ServiceBinder
	 * 
	 * @param device
	 */
	public void bindUPnPDevice(ServiceReference serviceReference, UPnPDevice device) {
		// TODO use serviceReference 
		synchronized (devices) {
			this.deviceServiceReferences.add(serviceReference);
			this.devices.add(device);
		}
	}
	
	/**
	 * for ServiceBinder
	 * 
	 * @param device
	 */
	public void unbindUPnPDevice(ServiceReference serviceReference, UPnPDevice device) {
		// TODO use serviceReference 
		synchronized (devices) {
			this.deviceServiceReferences.remove(serviceReference);
			this.devices.remove(device);
		}
	}
	
	/**
	 * for ServiceBinder
	 * 
	 * @param eventListener
	 */
	public void bindUPnPEventListener(ServiceReference serviceReference, UPnPEventListener eventListener) {
		// TODO use serviceReference 
		synchronized (eventListeners) {
			this.eventListenerServiceReferences.add(serviceReference);
			this.eventListeners.add(eventListener);
		}
	}
	
	/**
	 * for ServiceBinder
	 * 
	 * @param eventListener
	 */
	public void unbindUPnPEventListener(ServiceReference serviceReference, UPnPEventListener eventListener) {
		// TODO use serviceReference 
		synchronized (eventListeners) {
			this.eventListenerServiceReferences.remove(serviceReference);
			this.eventListeners.remove(eventListener);
		}
	}
	
	/**
	 * @return the usage string
	 * @see org.ungoverned.osgi.service.shell.Command#getUsage()
	 */
	public String getUsage() {
		return "list current UPnP devices";
	}

	/**
	 * @return
	 * @see org.ungoverned.osgi.service.shell.Command#getShortDescription()
	 */
	public String getShortDescription() {

		return "list current UPnP devices and associated devices";
	}


	@Override
	protected void execute() {
		// TODO Auto-generated method stub
		
	}
	
	//DONSEZ 
	
	private void help(PrintStream out, StringTokenizer st) {
		String command = HELP_CMD;
		if (st.hasMoreTokens()) {
			command = st.nextToken();
			if (command.equals(SERVICE_CMD)) {
				out.println("");
				out
						.println(getName()
								+ " "
								+ SERVICE_CMD
								+ " <ldap-filter> list UPnP services provided by device\n");
			} else if (command.equals(DEVICE_CMD)) {
				out.println("");
				out.println(getName() + " " + DEVICE_CMD
						+ " <ldap-filter> list UPnP devices\n");
			} else if (command.equals(LISTENER_CMD)) {
				out.println("");
				out.println(getName() + " " + LISTENER_CMD
						+ "                list UPnPListeners\n");
			} else {
				out.println("");
				out.println(getName() + " " + command
						+ " : this command do not available \n");
			}

		} else {
			out.println("");
			out.println(getName() + " " + HELP_CMD + " [ " + SERVICE_CMD
					+ " | " + DEVICE_CMD + " ]");
			out.println(getName() + " " + SERVICE_CMD + " <ldap-filter>");
			out.println(getName() + " " + DEVICE_CMD + " <ldap-filter>");
			out.println(getName() + " " + LISTENER_CMD);
			out.println("");
		}
	}

	private Dictionary setDic(UPnPService services) {

		Dictionary dictionary = new Properties();

		dictionary.put(UPnPService.ID, services.getId());
		dictionary.put(UPnPService.TYPE, services.getType());

		return dictionary;
	}
	
	
	
	/////////ENd Modif
	

}
