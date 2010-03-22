/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Rustem V. Rafikov
 */
package javax.imageio.spi;

import java.util.*;
import java.util.Map.Entry;

import org.apache.harmony.luni.util.NotImplementedException;

/**
 * TODO: add all the methods from the spec
 */
public class ServiceRegistry {

    CategoriesMap categories = new CategoriesMap(this);

    public ServiceRegistry(Iterator<Class<?>> categoriesIterator) {
        if (null == categoriesIterator) {
            throw new IllegalArgumentException("categories iterator should not be NULL");
        }
        while(categoriesIterator.hasNext()) {
            Class<?> c =  categoriesIterator.next();
            categories.addCategory(c);
        }
    }

    public static <T> Iterator<T> lookupProviders(Class<T> providerClass, ClassLoader loader) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public static <T> Iterator<T> lookupProviders(Class<T> providerClass) {
        return lookupProviders(providerClass, Thread.currentThread().getContextClassLoader());
    }

    public <T> boolean registerServiceProvider(T provider, Class<T> category) {
        return categories.addProvider(provider, category);
    }

    public void registerServiceProviders(Iterator<?> providers) {
        for (Iterator<?> iterator = providers; iterator.hasNext();) {
            categories.addProvider(iterator.next(), null);
        }
    }

    public void registerServiceProvider(Object provider) {
        categories.addProvider(provider, null);
    }

    public <T> boolean deregisterServiceProvider(T provider, Class<T> category) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public void deregisterServiceProvider(Object provider) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    @SuppressWarnings("unchecked")
    public <T> Iterator<T> getServiceProviders(Class<T> category, Filter filter, boolean useOrdering) {
        return new FilteredIterator<T>(filter, (Iterator<T>)categories.getProviders(category, useOrdering));
    }

    @SuppressWarnings("unchecked")
    public <T> Iterator<T> getServiceProviders(Class<T> category, boolean useOrdering) {
        return (Iterator<T>)categories.getProviders(category, useOrdering);
    }

    public <T> T getServiceProviderByClass(Class<T> providerClass) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public <T> boolean setOrdering(Class<T> category, T firstProvider, T secondProvider) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public <T> boolean unsetOrdering(Class<T> category, T firstProvider, T secondProvider) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public void deregisterAll(Class<?> category) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public void deregisterAll() throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    @Override
    public void finalize() throws Throwable {
        //TODO uncomment when deregisterAll is implemented
        //deregisterAll();
    }

    public boolean contains(Object provider) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public Iterator<Class<?>> getCategories() {
        return categories.list();
    }

    public static interface Filter {
        boolean filter(Object provider);
    }

    private static class CategoriesMap {
        Map<Class<?>, ProvidersMap> categories = new HashMap<Class<?>, ProvidersMap>();

        ServiceRegistry registry;

        public CategoriesMap(ServiceRegistry registry) {
            this.registry = registry;
        }

        //-- TODO: useOrdering
        Iterator<?> getProviders(Class<?> category, boolean useOrdering) {
            ProvidersMap providers = categories.get(category);
            if (null == providers) {
                throw new IllegalArgumentException("Unknown category: " + category);
            }
            return providers.getProviders(useOrdering);
        }

        Iterator<Class<?>> list() {
            return categories.keySet().iterator();
        }

        void addCategory(Class<?> category) {
            categories.put(category, new ProvidersMap());
        }

        /**
         * Adds a provider to the category. If <code>category</code> is
         * <code>null</code> then the provider will be added to all categories
         * which the provider is assignable from.
         * @param provider provider to add
         * @param category category to add provider to
         * @return if there were such provider in some category
         */
        boolean addProvider(Object provider, Class<?> category) {
            if (provider == null) {
                throw new IllegalArgumentException("provider should be != NULL");
            }

            boolean rt;
            if (category == null) {
                rt = findAndAdd(provider);
            } else {
                rt  = addToNamed(provider, category);
            }

            if (provider instanceof RegisterableService) {
                ((RegisterableService) provider).onRegistration(registry, category);
            }

            return rt;
        }

        private boolean addToNamed(Object provider, Class<?> category) {
            Object obj = categories.get(category);

            if (null == obj) {
                throw new IllegalArgumentException("Unknown category: " + category);
            }

            return ((ProvidersMap) obj).addProvider(provider);
        }

        private boolean findAndAdd(Object provider) {
            boolean rt = false;
            for (Entry<Class<?>, ProvidersMap> e : categories.entrySet()) {
                if (e.getKey().isAssignableFrom(provider.getClass())) {
                    rt |= e.getValue().addProvider(provider);
                }
            }
            return rt;
        }
    }

    private static class ProvidersMap {
        //-- TODO: providers ordering support

        Map<Class<?>, Object> providers = new HashMap<Class<?>, Object>();

        boolean addProvider(Object provider) {
            return providers.put(provider.getClass(), provider) != null;
        }

        Iterator<Class<?>> getProviderClasses() {
            return providers.keySet().iterator();
        }

        //-- TODO ordering
        Iterator<?> getProviders(boolean userOrdering) {
            return providers.values().iterator();
        }
    }

    private static class FilteredIterator<E> implements Iterator<E> {

        private Filter filter;
        private Iterator<E> backend;
        private E nextObj;

        public FilteredIterator(Filter filter, Iterator<E> backend) {
            this.filter = filter;
            this.backend = backend;
            findNext();
        }

        public E next() {
            if (nextObj == null) {
                throw new NoSuchElementException();
            }
            E tmp = nextObj;
            findNext();
            return tmp;
        }

        public boolean hasNext() {
            return nextObj != null;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Sets nextObj to a next provider matching the criterion given by the filter
         */
        private void findNext() {
            nextObj = null;
            while (backend.hasNext()) {
                E o = backend.next();
                if (filter.filter(o)) {
                    nextObj = o;
                    return;
                }
            }
        }
    }
}
