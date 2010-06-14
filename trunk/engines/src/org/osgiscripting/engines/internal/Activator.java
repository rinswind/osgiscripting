package org.osgiscripting.engines.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
  private ScriptEngineFactoryTracker tracker;
  
	public void start(BundleContext bc) throws Exception {
	  tracker = new ScriptEngineFactoryTracker(bc);	  
	  tracker.open();
	}

	public void stop(BundleContext context) throws Exception {
	  tracker.close();
	}
}
