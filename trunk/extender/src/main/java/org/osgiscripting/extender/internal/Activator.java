package org.osgiscripting.extender.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
  private ScriptEngineFactoryTracker engineTracker;
  private ScriptTracker scriptTracker;
  
  public void start(BundleContext bc) {
    this.engineTracker = new ScriptEngineFactoryTracker(bc);
    this.scriptTracker = new ScriptTracker(bc, engineTracker);
      
    engineTracker.open();
    scriptTracker.open();
  }
  
  public void stop(BundleContext bc) throws Exception {
    engineTracker.close();
    scriptTracker.close();
  }
  
//private final String language = "jruby";
//public void start(BundleContext context) throws Exception {
//  /*
//   * Here we construct our bridge classloader by referencing a jruby class
//   * directly.
//   * 
//   * FIX For a generic engine can't import any concrete class. Must maintain a
//   * registry of bundelized scripting engines and bridge two Bundle objects into
//   * the bridge class loader. I.e. the scripg bundle and the script engine
//   * bundle.
//   */
//  ClassLoader bridgeLoader = new BundleClassLoader(context.getBundle(), 
//      JRubyEngineFactory.class.getClassLoader());
//
//  final ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
//  try {
//    /*
//     * Current version of JRuby 1.5.0 uses context class loader a few places
//     * that affect this demo so we need to set the context class loader.
//     * Should be fixed in JRuby trunk however.
//     */
//    Thread.currentThread().setContextClassLoader(bridgeLoader);
//
//    /*
//     * Initialize Java6 script manager.
//     * 
//     * NOTE It's good that we can instantiate that. Bad that it needs a class
//     * loader bridge to locate the scripting engines. If the scripting engines
//     * are to be installed as bundles we must maintain a N-way bridge between
//     * N scripting engines and this scriptManager. Also we likely have to
//     * re-create the scriptManager every time the list of engines change since
//     * it likely scans the classpath for script engines only once.
//     * 
//     * Than there is the question what should we do when an engine disappears
//     * - we likely need to stop all bundles that use that engine.
//     */
//    ScriptEngineManager scriptManager = new ScriptEngineManager(bridgeLoader);
//    List<ScriptEngineFactory> factories = scriptManager.getEngineFactories();
//    for (ScriptEngineFactory f : factories) {
//      System.out.println("Found factory " + f.getEngineName() + " for " + f.getLanguageName());
//    }
//
//    // Retrieve just the engine we need.
//    ScriptEngine engine = scriptManager.getEngineByName(language);
//    
//    // The Ruby engine does implement Invocable
//    Invocable invocable = (Invocable) engine;
//
//    // Load a jruby script that instantiates an object impl. of our interface.
//    String script = "com/fortyoneconcepts/osgiscripting/Trial.rb";
//    Reader scriptReader = new BufferedReader(
//        new InputStreamReader(getClass().getClassLoader().getResourceAsStream(script)));
//
//    // Execute script
//    Object rawResult = engine.eval(scriptReader);
//
//    // Get reference to script's implementation of our interface.
//    ScriptRunner sample;
//    if (ScriptRunner.class.isInstance(rawResult)) {
//      sample = (ScriptRunner) rawResult;
//    } else {
//      sample = invocable.getInterface(rawResult, ScriptRunner.class);
//    }
//  } finally {
//    Thread.currentThread().setContextClassLoader(contextLoader);
//  }
//}
}
