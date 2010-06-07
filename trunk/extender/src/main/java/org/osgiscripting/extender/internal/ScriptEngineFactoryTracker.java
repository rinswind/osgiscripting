package org.osgiscripting.extender.internal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class ScriptEngineFactoryTracker implements BundleTrackerCustomizer {
  private final Map<String, ScriptEngineFactory> factories;
  private final BundleTracker tracker;

  public ScriptEngineFactoryTracker(BundleContext bc) {
    this.tracker = new BundleTracker(bc, Bundle.ACTIVE, this);
    this.factories = new ConcurrentHashMap<String, ScriptEngineFactory>();
  }

  public ScriptEngineFactory getScriptEngineFactory(String language) {
    return factories.get(language);
  }

  public void open() {
    // Add all engines found on the boot classpath
    ScriptEngineManager sm = new ScriptEngineManager();
    for (ScriptEngineFactory factory : sm.getEngineFactories()) {
      addFactory(factory);
    }
    
    // Dynamically track the rest
    tracker.open();
  }

  public void close() {
    factories.clear();
    tracker.close();
  }

  @Override
  public Object addingBundle(Bundle bundle, BundleEvent event) {
    ScriptEngineFactory factory = findFactory(bundle);
    if (factory == null) {
      return null;
    }
    
    addFactory(factory);
    return factory.getLanguageName();
  }

  @Override
  public void removedBundle(Bundle bundle, BundleEvent evnet, Object arg) {
    removeFactory((String) arg);
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
  
  private void addFactory(ScriptEngineFactory factory) {
    factories.put(factory.getLanguageName(), factory);
    
    System.out.printf(
        "Added factory\n" +
        "\tengine: %s:%s\n" +
        "\tlanguage: %s:%s\n",
        factory.getEngineName(),
        factory.getEngineVersion(),
        factory.getLanguageName(),
        factory.getLanguageVersion());
  }

  private void removeFactory(String arg) {
    factories.remove(arg);
  }
}
