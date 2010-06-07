package org.osgiscripting.extender.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;

public class BundleLoader extends ClassLoader {
  private final Bundle delegate;

  public BundleLoader(Bundle delegate) {
    this.delegate = delegate;
  }

  @Override
  public String toString() {
    return "BundleLoader[ " + delegate.getSymbolicName() + " ]";
  }

  @Override
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    System.out.println(this + ": loadClass(" + name + ", " + resolve + ")");
    
    Class<?> clazz = findClass(name);
    if (resolve) {
      resolveClass(clazz);
    }
    return clazz;
  }
  
  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    System.out.println(this + ": findClass(" + name + ")");
    return delegate.loadClass(name);
  }

  @Override
  public URL getResource(String name) {
    System.out.println(this + ": getResource(" + name + ")");
    return findResource(name);
  }

  @Override
  protected URL findResource(String name) {
    System.out.println(this + ": findResource(" + name + ")");
    return delegate.getResource(name);
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    System.out.println(this + ": getResources(" + name + ")");
    return findResources(name);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Enumeration<URL> findResources(String name) throws IOException {
    Enumeration res = delegate.getResources(name);
    System.out.println(this + ": findResources(" + name + "): " + res);
    return res;
  }
}