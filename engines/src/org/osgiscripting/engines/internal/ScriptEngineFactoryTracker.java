package org.osgiscripting.engines.internal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgiscripting.engines.Constants;

public class ScriptEngineFactoryTracker implements BundleTrackerCustomizer {
  private final BundleTracker tracker;
  private final BundleContext bc;
  
  public ScriptEngineFactoryTracker(BundleContext bc) {
    this.tracker = new BundleTracker(bc, Bundle.ACTIVE, this);
    this.bc = bc;
  }

  public void open() {
    // Add all engines found on the boot classpath
    ScriptEngineManager sm = new ScriptEngineManager();
    for (ScriptEngineFactory factory : sm.getEngineFactories()) {
      register(factory);
    }
    
    // Dynamically track the rest
    tracker.open();
  }

  public void close() {
    tracker.close();
  }

  @Override
  public Object addingBundle(Bundle bundle, BundleEvent event) {
    ScriptEngineFactory factory = findFactory(bundle);
    if (factory == null) {
      return null;
    }
    
    ServiceRegistration reg = register(factory);
    return reg;
  }

  @Override
  public void removedBundle(Bundle bundle, BundleEvent evnet, Object arg) {
    unrgister((ServiceRegistration) arg);
  }

  @Override
  public void modifiedBundle(Bundle bundle, BundleEvent event, Object arg) {
    /* Nothing to do */
  }

  private ScriptEngineFactory findFactory(Bundle bundle) {
    URL url = bundle.getEntry("META-INF/services/javax.script.ScriptEngineFactory");
    if (url == null) {
      return null;
    }
    
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
      try {
        for (String line = null; (line = reader.readLine()) != null;) {
          line = line.trim();
          
          if (line.startsWith("#")) {
            continue;
          }

          @SuppressWarnings("unchecked")
          Class<ScriptEngineFactory> clazz = bundle.loadClass(line);
          
          return clazz.newInstance();
        }
      } finally {
        reader.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return null;
  }
  
  private ServiceRegistration register(ScriptEngineFactory factory) {
    Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put(Constants.ENGINE_NAME, factory.getEngineName());
    props.put(Constants.ENGINE_VERSION, factory.getEngineVersion());
    props.put(Constants.LANGUAGE_NAME, factory.getLanguageName());
    props.put(Constants.LANGUAGE_VERSION, factory.getLanguageVersion());
    
    ServiceRegistration reg = bc.registerService(
        ScriptEngineFactory.class.getName(), factory, props);
    
    return reg;
  }

  private void unrgister(ServiceRegistration reg) {
    reg.unregister();
  }
}
