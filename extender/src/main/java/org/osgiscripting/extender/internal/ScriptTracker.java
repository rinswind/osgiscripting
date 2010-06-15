package org.osgiscripting.extender.internal;

import static org.osgiscripting.extender.Constants.SCRIPT_FILE;
import static org.osgiscripting.extender.Constants.SCRIPT_LANGUAGE;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgiscripting.apitest.ScriptService;

public class ScriptTracker implements BundleTrackerCustomizer {
  private final BundleTracker tracker;
  private final EngineTracker engineTracker;
  
  public ScriptTracker(BundleContext bc, EngineTracker engineTracker) {
    this.tracker = new BundleTracker(bc, Bundle.ACTIVE, this);
    this.engineTracker = engineTracker;
  }

  public void open() {
    tracker.open();
  }

  public void close() {
    tracker.close();
  }

  @Override
  public Object addingBundle(Bundle bundle, BundleEvent event) {
    URL script = findScript(bundle);
    if (script == null) {
      return null;
    }

    ScriptEngineFactory factory = findFactory(bundle);
    if (factory == null) {
      return null;
    }

    try {
      ScriptService s = execute(script, ScriptService.class, factory, bundle);
      
      System.out.println("Execution result: " + s.run("42"));
    } catch (Exception exc) {
      exc.printStackTrace();
    }

    return null;
  }

  @Override
  public void removedBundle(Bundle bundle, BundleEvent event, Object arg) {
    /* Nothing to do */
  }

  @Override
  public void modifiedBundle(Bundle bundle, BundleEvent event, Object arg) {
    /* Nothing to do */
  }

  private URL findScript(Bundle bundle) {
    String scriptFile = (String) bundle.getHeaders().get(SCRIPT_FILE);
    if (scriptFile == null) {
      return null;
    }

    scriptFile = scriptFile.trim();
    if (scriptFile.length() == 0) {
      return null;
    }

    return bundle.getResource(scriptFile);
  }

  private ScriptEngineFactory findFactory(Bundle bundle) {
    String language = (String) bundle.getHeaders().get(SCRIPT_LANGUAGE);
    return engineTracker.getFactory(language);
  }

  private <T> T execute(URL script, Class<T> resType, ScriptEngineFactory factory,
      Bundle scriptBundle) throws Exception {

    ScriptEngine engine;
    Object result;

    // Execute script with a bridge context loader
    ClassLoader context = Thread.currentThread().getContextClassLoader();
    try {
      BundleClassLoader bridge = new BundleClassLoader(
          scriptBundle, factory.getClass().getClassLoader());
      Thread.currentThread().setContextClassLoader(bridge);
      
      /*
       * Must make engine within the current context loader because it picks it
       * up to load resources and classes. E.g. we pass the bridge to the ruby
       * runtime. Each invocation makes a new engine so we won't have clashes
       * with other executing scripts.
       */
      engine = factory.getScriptEngine();
      
      BufferedReader reader = new BufferedReader(new InputStreamReader(script.openStream(), "UTF-8"));
      
      result = engine.eval(reader);
    } finally {
      Thread.currentThread().setContextClassLoader(context);
    }

    // Convert the result
    if (resType.isInstance(result)) {
      return resType.cast(result);
    }

    if (engine instanceof Invocable) {
      return ((Invocable) engine).getInterface(result, resType);
    }

    throw new RuntimeException("Can't obtain result");
  }
}
