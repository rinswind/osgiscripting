package org.osgiscripting.extender.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
  private EngineTracker engineTracker;
  private ScriptTracker scriptTracker;
  
  public void start(BundleContext bc) {
    this.engineTracker = new EngineTracker(bc);
    this.scriptTracker = new ScriptTracker(bc, engineTracker);
      
    engineTracker.open();
    scriptTracker.open();
  }
  
  public void stop(BundleContext bc) throws Exception {
    engineTracker.close();
    scriptTracker.close();
  }
}
