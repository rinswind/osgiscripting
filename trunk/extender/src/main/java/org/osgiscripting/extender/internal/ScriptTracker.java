package org.osgiscripting.extender.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgiscripting.extender.Constants;
import org.osgiscripting.extender.ScriptService;

public class ScriptTracker implements BundleTrackerCustomizer {
  private final BundleTracker tracker;
  private final ScriptEngineFactoryTracker engineTracker;
  
  public ScriptTracker(BundleContext bc, ScriptEngineFactoryTracker engineTracker) {
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
    String script = findScript(bundle);
    if (script == null) {
      return null;
    }

    ScriptEngineFactory factory = findFactory(bundle);
    if (factory == null) {
      return null;
    }

    try {
      executeScript(script, factory, bundle);
    } catch (ScriptException exc) {
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

  private String findScript(Bundle bundle) {
    String scriptFile = (String) bundle.getHeaders().get(Constants.SCRIPT_FILE);
    if (scriptFile == null) {
      return null;
    }

    scriptFile = scriptFile.trim();
    if (scriptFile.length() == 0) {
      return null;
    }

    URL scriptUrl = bundle.getResource(scriptFile);
    if (scriptUrl == null) {
      return null;
    }

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(scriptUrl.openStream()));
      try {
        StringBuilder script = new StringBuilder();

        for (String line = null; (line = reader.readLine()) != null;) {
          script.append(line).append('\n');
        }

        return script.toString();
      } finally {
        reader.close();
      }
    } catch (IOException exc) {
      exc.printStackTrace();
    }

    return null;
  }

  private ScriptEngineFactory findFactory(Bundle bundle) {
    String scriptLangauge = (String) bundle.getHeaders().get(Constants.SCRIPT_LANGUAGE);
    return engineTracker.getScriptEngineFactory(scriptLangauge);
  }

  private Object executeScript(String script, ScriptEngineFactory factory, Bundle scriptBundle)
      throws ScriptException {

    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    try {
      /*
       * Set the context class loader just in case the engine needs it to load
       * resources from the script bundle.
       */
      Thread.currentThread().setContextClassLoader(new BundleLoader(scriptBundle));

      ScriptEngine engine = factory.getScriptEngine();
      Object res = engine.eval(script);

      // Get reference to script's implementation of our interface.
      ScriptService sample;
      if (res instanceof ScriptService) {
        sample = (ScriptService) res;
      } else if (engine instanceof Invocable) {
        sample = ((Invocable) engine).getInterface(res, ScriptService.class);
      } else {
        throw new ScriptException("Unexpected script result " + res);
      }
      
      System.out.println("Execution result: " + sample.run(42));

      return res;
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }
  }
}
