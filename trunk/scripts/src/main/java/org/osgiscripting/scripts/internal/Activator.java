package org.osgiscripting.scripts.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
  private ScriptTracker scriptTracker;
  
  public void start(BundleContext bc) {
    this.scriptTracker = new ScriptTracker(bc);
    scriptTracker.open();
  }
  
  public void stop(BundleContext bc) throws Exception {
    scriptTracker.close();
  }
}
