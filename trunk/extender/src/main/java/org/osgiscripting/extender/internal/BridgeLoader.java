package org.osgiscripting.extender.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;

/**
 * Bridge class loader that makes sure all files are accessible from both our
 * bundle and another bundles' classloader. In particular it makes sure that
 * findResources can find files outside the package structure in scripting
 * impls.
 * 
 * @author mmc
 */
public class BridgeLoader extends ClassLoader {
  private static final Logger LOG = Logger.getLogger(BridgeLoader.class.getName());
  
  private final Bundle delegate;
  private final ClassLoader other;

  public BridgeLoader(Bundle delegate) {
    this(delegate, null);
  }

  public BridgeLoader(Bundle delegate, ClassLoader parent) {
    super(parent);
    this.other = parent;
    this.delegate = delegate;
  }

  @Override
  public String toString() {
    return "Bridge[ " + delegate + ", " + other + "]";
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public Enumeration<URL> findResources(String name) throws IOException {
    Set<URL> set = new HashSet<URL>();
    
    addToSet(set, delegate.getResources(name));
    addToSet(set, other.getResources(name));
    
    if (set.isEmpty()) {
      LOG.log(Level.FINEST, "Call to Bundle.getResources(" + name + ") returned null");
    }

    return Collections.enumeration(set);
  }
  
  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    return findResources(name);
  }

  @Override
  public URL findResource(String name) {
    return delegate.getResource(name);
  }

  @Override
  public Class<?> findClass(String name) throws ClassNotFoundException {
    return delegate.loadClass(name);
  }

  @Override
  public URL getResource(String name) {
    URL result = (other == null) ? findResource(name) : super.getResource(name);
    if (result == null) {
      LOG.log(Level.WARNING, "Call to Bundle.getResource(" + name + ") returned null");
    }
    return result;
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> clazz = (other == null) ? findClass(name) : super.loadClass(name, false);
    if (resolve) {
      super.resolveClass(clazz);
    }
    return clazz;
  }
  
  private static <T> void addToSet(Set<T> set, Enumeration<T> enumeration) {
    if (enumeration != null) {
      while (enumeration.hasMoreElements()) {
        set.add(enumeration.nextElement());
      }
    }
  }
}
