package org.osgiscripting.scripts.internal;

import static org.osgiscripting.engines.Constants.LANGUAGE_NAME;
import static org.osgiscripting.scripts.Constants.SCRIPT_FILE;
import static org.osgiscripting.scripts.Constants.SCRIPT_LANGUAGE;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgiscripting.apitest.ScriptService;

public class ScriptTracker implements BundleTrackerCustomizer {
  private final BundleTracker tracker;
  private final BundleContext bc;
  
  public ScriptTracker(BundleContext bc) {
    this.tracker = new BundleTracker(bc, Bundle.ACTIVE, this);
    this.bc = bc;
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
    System.out.println(String.format("findScript(%s:%s)", bundle.getSymbolicName(), bundle.getHeaders().get(SCRIPT_FILE)));
    
    String scriptFile = (String) bundle.getHeaders().get(SCRIPT_FILE);
    if (scriptFile == null) {
      System.out.println("findScript: no script file");
      return null;
    }

    scriptFile = scriptFile.trim();
    if (scriptFile.length() == 0) {
      System.out.println("findScript: no script");
      return null;
    }

    URL res = bundle.getResource(scriptFile);
    System.out.println("findScript: " + res);
    return res;
  }

  private ScriptEngineFactory findFactory(Bundle bundle) {
    System.out.println(String.format("findFactory(%s:%s)", bundle.getSymbolicName(), bundle.getHeaders().get(SCRIPT_LANGUAGE)));
    
    ServiceReference[] refs;
    try {
      refs = bc.getServiceReferences(
          ScriptEngineFactory.class.getName(), 
          // Do a case insensitive search
          "(" + LANGUAGE_NAME + "~=" + bundle.getHeaders().get(SCRIPT_LANGUAGE) + ")");
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException("unexpected", e);
    }
    
    if (refs == null) {
      System.out.println("findFactory: null");
      return null;
    }
    
    ScriptEngineFactory res = (ScriptEngineFactory) bc.getService(refs[0]);
    System.out.println("findFactory: " + res);
    return res;
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
