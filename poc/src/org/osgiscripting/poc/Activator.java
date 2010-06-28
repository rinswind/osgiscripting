package org.osgiscripting.poc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgiscripting.apitest.ScriptService;

/**
 * Proof Of Concept bundle. Locates the ruby engine, than locates a test script,
 * than executes the script through the engine. Bridges the class loaders of the
 * engine and the script allowing the engine to load resources from the script
 * bundle.
 * 
 * @author mmc
 * @author tbb
 */
public class Activator implements BundleActivator {
  private BundleContext bc;

  public void start(BundleContext bc) throws Exception {
    this.bc = bc;

    // Find engine factory
    Bundle engineBundle = findBundle("org.jruby.jruby");
    ScriptEngineFactory factory = getFactory(engineBundle);
    
    // Find script
    Bundle scriptBundle = findBundle("org.osgiscripting.rubytest");
    URL script = getScript(scriptBundle);
    
    // Execute script
    ScriptService sample = execute(script, ScriptService.class, factory, scriptBundle);
    
    // Use script result
    System.out.println("Invoking " + sample.run("42"));
  }

  public void stop(BundleContext context) throws Exception {
  }

  private ScriptEngineFactory getFactory(BundleContext bc) {
    ServiceReference[] refs;
    try {
      refs = bc.getServiceReferences(
          ScriptEngineFactory.class.getName(), "(language-name=ruby)");
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException("unexpected", e);
    }
    
    if (refs == null) {
      return null;
    }
    
    return (ScriptEngineFactory) bc.getService(refs[0]);
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

      result = engine.eval(createBufferedReader(script));
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

  private Bundle findBundle(String symbolicName) {
    for (Bundle b : bc.getBundles()) {
      if (b.getSymbolicName().equals(symbolicName)) {
        return b;
      }
    }
    return null;
  }

  private URL getScript(Bundle bundle) {
    String scriptFile = (String) bundle.getHeaders().get("Script-File");
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

    return scriptUrl;
  }

  private ScriptEngineFactory getFactory(Bundle bundle) {
    URL url = bundle.getEntry("META-INF/services/javax.script.ScriptEngineFactory");
    if (url == null) {
      return null;
    }

    try {
      BufferedReader reader = createBufferedReader(url);
      try {
        for (String line = null; (line = reader.readLine()) != null;) {
          line = line.trim();

          if (line.startsWith("#")) {
            continue;
          }
          
          // The first non-comment line is the one with the engine class name.
          @SuppressWarnings("unchecked")
          Class<ScriptEngineFactory> type = bundle.loadClass(line);
          return type.newInstance();
        }
      } finally {
        reader.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }
  
  private static BufferedReader createBufferedReader(URL script) throws IOException {
    return new BufferedReader(new InputStreamReader(script.openStream(), "UTF-8"));
  }
}
