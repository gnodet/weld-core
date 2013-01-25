/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.environment.osgi.impl.integration.discovery;

import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.environment.osgi.impl.extension.context.ContextHolder;
import org.jboss.weld.environment.osgi.impl.integration.OSGiProxyService;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

import javax.enterprise.inject.spi.Extension;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Implements the basic requirements of a {@link Deployment}. Provides a service
 * registry.
 * <p/>
 * Suitable for extension by those who need to build custom {@link Deployment}
 * implementations.
 *
 * @author Mathieu ANCELIN - SERLI (mathieu.ancelin@serli.com)
 * @author Matthieu CLOCHARD - SERLI (matthieu.clochard@serli.com)
 */
public abstract class AbstractWeldOSGiDeployment implements Deployment {
    private final ServiceRegistry serviceRegistry;

    private final ArrayList<Metadata<Extension>> extensions;

    private final Bundle bundle;

    public AbstractWeldOSGiDeployment(Bootstrap bootstrap, Bundle bundle) {
        this.serviceRegistry = new SimpleServiceRegistry();
        this.serviceRegistry.add(ProxyServices.class, new OSGiProxyService());
        this.bundle = bundle;
        // OK, Here we can install our own Extensions instances
        Set<Bundle> bundles = getDeploymentBundles(bundle);
        this.extensions = new ArrayList<Metadata<Extension>>();
        for (Bundle b : bundles) {
            loadExtensions(bootstrap, new BridgeClassLoader(b));
        }
    }

    private Set<Bundle> getDeploymentBundles(Bundle bundle) {
        Set<Bundle> bundles = new HashSet<Bundle>();
        ServiceReference sr = bundle.getBundleContext().getServiceReference(PackageAdmin.class.getName());
        if (sr != null) {
            try {
                PackageAdmin pa = (PackageAdmin) bundle.getBundleContext().getService(sr);
                RequiredBundle[] rqs = pa.getRequiredBundles(null);
                for (RequiredBundle rq : rqs) {
                    Bundle[] rbs = rq.getRequiringBundles();
                    if (rbs != null) {
                        for (Bundle rb : rbs) {
                            if (rb == bundle) {
                                bundles.add(rq.getBundle());
                                break;
                            }
                        }
                    }
                }
            } finally {
                bundle.getBundleContext().ungetService(sr);
            }
        }
        bundles.add(bundle);
        bundles.add(FrameworkUtil.getBundle(ContextHolder.class));
        bundles.add(FrameworkUtil.getBundle(getClass()));
        return bundles;
    }

    private void loadExtensions(Bootstrap bootstrap, ClassLoader loader) {
        Iterable<Metadata<Extension>> exts = bootstrap.loadExtensions(loader);
        if (exts != null) {
            for (Metadata<Extension> ext : exts) {
                extensions.add(ext);
            }
        }
    }

    @Override
    public ServiceRegistry getServices() {
        return serviceRegistry;
    }

    @Override
    public Iterable<Metadata<Extension>> getExtensions() {
        return extensions;
    }

    private static class BridgeClassLoader extends ClassLoader {
        private final Bundle bundle;

        public BridgeClassLoader(Bundle bundle) {
            this.bundle = bundle;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return bundle.loadClass(name);
        }

        @Override
        public Enumeration<URL> getResources(String s) throws IOException {
            Enumeration<URL> resources =  bundle.getResources(s);
            if (resources == null) {
                resources = Collections.emptyEnumeration();
            }
            return resources;
        }
    }

}
