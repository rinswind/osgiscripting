package org.osgiscripting.poc;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;

/**
 * Bridge class loader that makes sure all files are accessible from both our
 * bundle and another bundles' ClassLoader. In particular it makes sure that
 * {@link #findResources} can find files outside the package structure in
 * scripting implementations.
 * 
 * All loads are first delegate to the parent class loader and if filed will
 * look unto the delegate bundle.
 * 
 * @author mmc
 * @author tbb
 */
public class BundleClassLoader extends ClassLoader {
  private final Bundle delegate;

  public BundleClassLoader(Bundle delegate) {
    this (delegate, null);
  }
  
  public BundleClassLoader(Bundle delegate, ClassLoader parent) {
    super(parent);
    this.delegate = delegate;
  }

  @Override
  public String toString() {
    return "Bridge[ " + delegate + ", " + getParent() + "]";
  }
  
  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    System.out.println(this + ": loadClass(" + name + ", " + resolve + ")");
    return super.loadClass(name, resolve);
  }
  
  @Override
  public Class<?> findClass(String name) throws ClassNotFoundException {
    System.out.println(this + ": findClass(" + name + ")");
    return delegate.loadClass(name);
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    System.out.println(this + ": getResources(" + name + ")");
    return super.getResources(name);
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public Enumeration<URL> findResources(String name) throws IOException {
    System.out.println(this + ": findResources(" + name + ")");
    return delegate.getResources(name);
  }
  
  @Override
  public URL getResource(String name) {
    System.out.println(this + ": getResource(" + name + ")");
    return super.getResource(name);
  }
  
  @Override
  public URL findResource(String name) {
    System.out.println(this + ": findResource(" + name + ")");
    return delegate.getResource(name);
  }
}
