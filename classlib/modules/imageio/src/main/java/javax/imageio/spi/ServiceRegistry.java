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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

import org.apache.harmony.luni.util.NotImplementedException;
import org.apache.harmony.x.imageio.internal.nls.Messages;

public class ServiceRegistry {

    CategoriesMap categories = new CategoriesMap(this);

    public ServiceRegistry(Iterator<Class<?>> categoriesIterator) {
        if (null == categoriesIterator) {
            throw new IllegalArgumentException(Messages.getString("imageio.5D"));
        }
        while(categoriesIterator.hasNext()) {
            Class<?> c =  categoriesIterator.next();
            categories.addCategory(c);
        }
    }

    public static <T> Iterator<T> lookupProviders(Class<T> providerClass, ClassLoader loader) {
        return new LookupProvidersIterator(providerClass, loader);
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

    public <T> boolean deregisterServiceProvider(T provider, Class<T> category) {
        return categories.removeProvider(provider, category);
    }

    public void deregisterServiceProvider(Object provider) {
        categories.removeProvider(provider);
    }

//    @SuppressWarnings("unchecked")
    public <T> Iterator<T> getServiceProviders(Class<T> category, Filter filter, boolean useOrdering) {
        return new FilteredIterator<T>(filter, (Iterator<T>) categories.getProviders(category, useOrdering));
    }

//    @SuppressWarnings("unchecked")
    public <T> Iterator<T> getServiceProviders(Class<T> category, boolean useOrdering) {
        return (Iterator<T>) categories.getProviders(category, useOrdering);
    }

    public <T> T getServiceProviderByClass(Class<T> providerClass) {
        return categories.getServiceProviderByClass(providerClass);
    }

    public <T> boolean setOrdering(Class<T> category, T firstProvider, T secondProvider) {
        return categories.setOrdering(category, firstProvider, secondProvider);
    }

    public <T> boolean unsetOrdering(Class<T> category, T firstProvider, T secondProvider) {
        return categories.unsetOrdering(category, firstProvider, secondProvider);
    }

    public void deregisterAll(Class<?> category) {
        categories.removeAll(category);
    }

    public void deregisterAll() {
        categories.removeAll();
    }

    @Override
    public void finalize() throws Throwable {
        deregisterAll();
    }

    public boolean contains(Object provider) {
        if (provider == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.5E"));
        }
        
        return categories.contains(provider);
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

        boolean contains(Object provider) {
            for (Map.Entry<Class<?>, ProvidersMap> e : categories.entrySet()) {
                ProvidersMap providers = e.getValue();
                if (providers.contains(provider)) {
                    return true;
                }
            }
            
            return false;
        }

        <T> boolean setOrdering(Class<T> category, T firstProvider, T secondProvider) {
            ProvidersMap providers = categories.get(category);
            
            if (providers == null) {
                throw new IllegalArgumentException(Messages.getString("imageio.92", category));
            }
            
            return providers.setOrdering(firstProvider, secondProvider);
        }
        
        <T> boolean unsetOrdering(Class<T> category, T firstProvider, T secondProvider) {
            ProvidersMap providers = categories.get(category);
            
            if (providers == null) {
                throw new IllegalArgumentException(Messages.getString("imageio.92", category));
            }
            
            return providers.unsetOrdering(firstProvider, secondProvider);
        }
        
        Iterator<?> getProviders(Class<?> category, boolean useOrdering) {
            ProvidersMap providers = categories.get(category);
            if (null == providers) {
                throw new IllegalArgumentException(Messages.getString("imageio.92", category));
            }
            return providers.getProviders(useOrdering);
        }
        
        <T> T getServiceProviderByClass(Class<T> providerClass) {
        	for (Map.Entry<Class<?>, ProvidersMap> e : categories.entrySet()) {
        		if (e.getKey().isAssignableFrom(providerClass)) {
        			T provider = e.getValue().getServiceProviderByClass(providerClass);
        			if (provider != null) {
        				return provider;
        			}
        		}
        	}
        	return null;
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
                throw new IllegalArgumentException(Messages.getString("imageio.5E"));
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
            if (!category.isAssignableFrom(provider.getClass())) {
                throw new ClassCastException();
            }
            Object obj = categories.get(category);

            if (null == obj) {
                throw new IllegalArgumentException(Messages.getString("imageio.92", category));
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
        
        boolean removeProvider(Object provider, Class<?> category) {
            if (provider == null) {
                throw new IllegalArgumentException(Messages.getString("imageio.5E"));
            }
            
            if (!category.isAssignableFrom(provider.getClass())) {
                throw new ClassCastException();
            }
            
            Object obj = categories.get(category);
            
            if (obj == null) {
                throw new IllegalArgumentException(Messages.getString("imageio.92", category));
            }
            
            return ((ProvidersMap) obj).removeProvider(provider, registry, category);
        }
        
        void removeProvider(Object provider) {
            if (provider == null) {
                throw new IllegalArgumentException(Messages.getString("imageio.5E"));
            }
            
            for (Entry<Class<?>, ProvidersMap> e : categories.entrySet()) {
                ProvidersMap providers = e.getValue();
                providers.removeProvider(provider, registry, e.getKey());
            }
        }
        
        void removeAll(Class<?> category) {
            Object obj = categories.get(category);
            
            if (obj == null) {
                throw new IllegalArgumentException(Messages.getString("imageio.92", category));
            }
            
            ((ProvidersMap) obj).clear(registry);         
        }
        
        void removeAll() {
            for ( Map.Entry<Class<?>, ProvidersMap> e : categories.entrySet()) {
                removeAll(e.getKey());                
            }
        }
    }

    private static class ProvidersMap {

        Map<Class<?>, Object> providers = new HashMap<Class<?>, Object>();
        Map<Object, ProviderNode> nodeMap = new HashMap<Object, ProviderNode>();

        boolean addProvider(Object provider) {
            ProviderNode node =  new ProviderNode(provider);
            nodeMap.put(provider, node);
            Object obj = providers.put(provider.getClass(), provider);
            
            if (obj !=  null) {
                nodeMap.remove(obj);
                return false;
            }
            
            return true;
        }

        boolean contains(Object provider) {
            return providers.containsValue(provider);
        }

        boolean removeProvider(Object provider,
                ServiceRegistry registry, Class<?> category) {
            
            Object obj = providers.get(provider.getClass());
            if ((obj == null) || (obj != provider)) {
                return false;
            }
            
            providers.remove(provider.getClass());
            nodeMap.remove(provider);
            
            if (provider instanceof RegisterableService) {
                ((RegisterableService) provider).onDeregistration(registry, category);
            }            
            
            return true;
        }
        
        void clear(ServiceRegistry registry) {
            for (Map.Entry<Class<?>, Object> e : providers.entrySet()) {
                Object provider = e.getValue();
                
                if (provider instanceof RegisterableService) {
                    ((RegisterableService) provider).onDeregistration(registry, e.getKey());
                }
            }
            
            providers.clear();
            nodeMap.clear();
        }

        Iterator<Class<?>> getProviderClasses() {
            return providers.keySet().iterator();
        }

        Iterator<?> getProviders(boolean useOrdering) {
            if (useOrdering) {
                return new OrderedProviderIterator(nodeMap.values().iterator());              
            }
            
            return providers.values().iterator();
        }
        
        @SuppressWarnings("unchecked")
		<T> T getServiceProviderByClass(Class<T> providerClass) {
        	return (T)providers.get(providerClass);
        }
        
        public <T> boolean setOrdering(T firstProvider, T secondProvider) {
            if (firstProvider == secondProvider) {
                throw new IllegalArgumentException(Messages.getString("imageio.98"));
            }
            
            if ((firstProvider == null) || (secondProvider == null)) {
                throw new IllegalArgumentException(Messages.getString("imageio.5E"));
            }
           
            ProviderNode firstNode = nodeMap.get(firstProvider);
            ProviderNode secondNode = nodeMap.get(secondProvider);
                    
            // if the ordering is already set, return false
            if ((firstNode == null) || (firstNode.contains(secondNode))) {
                return false;
            }
            
            // put secondProvider into firstProvider's outgoing nodes list
            firstNode.addOutEdge(secondNode);
            // increase secondNode's incoming edge by 1
            secondNode.addInEdge();         
            
            return true;
        }
        
        public <T> boolean unsetOrdering(T firstProvider, T secondProvider) {
            if (firstProvider == secondProvider) {
                throw new IllegalArgumentException(Messages.getString("imageio.98"));
            }
            
            if ((firstProvider == null) || (secondProvider == null)) {
                throw new IllegalArgumentException(Messages.getString("imageio.5E"));
            }
            
            ProviderNode firstNode = nodeMap.get(firstProvider);
            ProviderNode secondNode = nodeMap.get(secondProvider); 
                    
            // if the ordering is not set, return false
            if ((firstNode == null) || (!firstNode.contains(secondNode))) {
                return false;
            }
                    
            // remove secondProvider from firstProvider's outgoing nodes list
            firstNode.removeOutEdge(secondNode);
            // decrease secondNode's incoming edge by 1
            secondNode.removeInEdge();
                    
            return true;            
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
    
    private static class ProviderNode {
        // number of incoming edges
        private int incomingEdges;  
        // all outgoing nodes
        private Set<Object> outgoingNodes; 
        private Object provider;
                
        public ProviderNode(Object provider) {
            incomingEdges = 0;
            outgoingNodes = new HashSet<Object>();
            this.provider = provider;
        }
            
        public Object getProvider() {
            return provider;
        }
        
        public Iterator<Object> getOutgoingNodes() {
            return outgoingNodes.iterator();
        }
        
        public boolean addOutEdge(Object secondProvider) {
            return outgoingNodes.add(secondProvider);
        }
        
        public <T> boolean removeOutEdge(Object provider) {
            return outgoingNodes.remove(provider);
        }
        
        public void addInEdge() {
            incomingEdges++;
        }
        
        public void removeInEdge() {
            incomingEdges--;
        }
        
        public int getIncomingEdges() {
            return incomingEdges;
        }
        
        public boolean contains(Object provider) {
            return outgoingNodes.contains(provider);
        }
    }

    /**
     * The iterator implements Kahn topological sorting algorithm.
     * @see <a href="http://en.wikipedia.org/wiki/Topological_sorting">Wikipedia</a>
     * for further reference.
     */
    private static class OrderedProviderIterator implements Iterator {

        // the stack contains nodes which has no lesser nodes
        // except those already returned by the iterator
        private Stack<ProviderNode> firstNodes = new Stack<ProviderNode>();

        // a dynamic counter of incoming nodes
        // when a node is returned by iterator, the counters for connected
        // nodes decrement
        private Map<ProviderNode, Integer> incomingEdges = new HashMap<ProviderNode, Integer>();
        
        public OrderedProviderIterator(Iterator it) {
            // find all the nodes that with no incoming edges and
            // add them to firstNodes
            while (it.hasNext()) {
                ProviderNode node = (ProviderNode) it.next();
                incomingEdges.put(node, new Integer(node.getIncomingEdges()));
                if (node.getIncomingEdges() == 0) {
                    firstNodes.push(node);
                }
            }
        }
            
        public boolean hasNext() {
            return !firstNodes.empty();
        }

        public Object next() {
            if (firstNodes.empty()) {
               throw new NoSuchElementException();
            }
                            
            // get a node from firstNodes
            ProviderNode node = firstNodes.pop();
                            
            // find all the outgoing nodes
            Iterator it = node.getOutgoingNodes();
            while (it.hasNext()) {
                ProviderNode outNode = (ProviderNode) it.next();
                
                // remove the incoming edge from the node.
                int edges = incomingEdges.get(outNode);
                edges--;
                incomingEdges.put(outNode, new Integer(edges));
                
                // add to the firstNodes if this node's incoming edge is equal to 0
                if (edges == 0) {
                    firstNodes.push(outNode);
                }
            }
            
            incomingEdges.remove(node);
                            
            return node.getProvider();
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    static class LookupProvidersIterator<T> implements Iterator {

        private Set<String> providerNames = new HashSet<String>();
        private Iterator<String> it = null;
        private ClassLoader loader = null;
        
        public LookupProvidersIterator(Class<T> providerClass, ClassLoader loader) {
            this.loader = loader;
            
            Enumeration<URL> e = null;
            try {
                e = loader.getResources("META-INF/services/"+providerClass.getName()); //$NON-NLS-1$
                while (e.hasMoreElements()) {
                    Set<String> names = parse((URL)e.nextElement());
                    providerNames.addAll(names);
                }
            } catch (IOException e1) {
                // Ignored
            }

            it = providerNames.iterator();
        }
        
        /*
         * Parse the provider-configuration file as specified 
         * @see <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jar/jar.html#Provider Configuration File">JAR File Specification</a>
         */
        private Set<String> parse(URL u) {
            InputStream input = null;
            BufferedReader reader = null;
            Set<String> names = new HashSet<String>();
            try {
                input = u.openStream();
                reader = new BufferedReader(new InputStreamReader(input, "utf-8")); //$NON-NLS-1$
                
                String line;
                while ((line = reader.readLine()) != null) {
                    // The comment character is '#' (0x23)
                    // on each line all characters following the first comment character are ignored
                    int sharpIndex = line.indexOf('#');
                    if (sharpIndex>=0) {
                        line = line.substring(0, sharpIndex);
                    }
                    
                    // Whitespaces are ignored
                    line = line.trim();
                    
                    if (line.length()>0) {
                        // a java class name, check if identifier correct
                        char[] namechars = line.toCharArray();
                        for (int i = 0; i < namechars.length; i++) {
                            if (!(Character.isJavaIdentifierPart(namechars[i]) || namechars[i] == '.')) {
                                throw new ServiceConfigurationError(Messages.getString("imageio.99", line));
                            }
                        }
                        names.add(line);
                    }
                }
            } catch (IOException e) {
                throw new ServiceConfigurationError(e.toString());
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException e) {
                    throw new ServiceConfigurationError(e.toString());
                }
            }
            
            return names;
        }
        
        public boolean hasNext() {
            return it.hasNext();
        }

        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            
            String name = (String)it.next();
            try {
                return Class.forName(name, true, loader).newInstance();
            } catch (Exception e) {
                throw new ServiceConfigurationError(e.toString());
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();   
        }
    }
    
    /**
     * An Error that can be thrown when something wrong occurs in loading a service
     * provider.
     */
    static class ServiceConfigurationError extends Error {
        
        private static final long serialVersionUID = 74132770414881L;

        /**
         * The constructor
         * 
         * @param msg
         *            the message of this error
         */
        public ServiceConfigurationError(String msg) {
            super(msg);
        }

        /**
         * The constructor
         * 
         * @param msg
         *            the message of this error
         * @param cause 
         *            the cause of this error
         */
        public ServiceConfigurationError(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
