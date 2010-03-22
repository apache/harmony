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

package java.lang;

import java.net.URL;
import java.net.JarURLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Hashtable;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.lang.ref.SoftReference;
import java.lang.reflect.AnnotatedElement;
import java.lang.annotation.Annotation;

import org.apache.harmony.vm.VMGenericsAndAnnotations;
import org.apache.harmony.vm.VMStack;

/**
 * @com.intel.drl.spec_ref 
 */
public class Package implements AnnotatedElement {
    
	/**
	 *
	 *  @com.intel.drl.spec_ref
	 * 
	 **/
    public Annotation[] getDeclaredAnnotations() {
		Class pc = null;
		try {
            //default package cannot be annotated
			pc = Class.forName(getName() + ".package-info", false, loader);
		} catch (ClassNotFoundException _) {
			return new Annotation[0];
		}
		return VMGenericsAndAnnotations.getDeclaredAnnotations(pc); // get all annotations directly present on this element
    }

	/**
	 *
	 *  @com.intel.drl.spec_ref
	 * 
	 **/
    public Annotation[] getAnnotations() {
		return getDeclaredAnnotations();
    }

	/**
	 *
	 *  @com.intel.drl.spec_ref
	 * 
	 **/
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		if(annotationClass == null) {
			throw new NullPointerException();
		}
		Annotation aa[] = getAnnotations();
		for (int i = 0; i < aa.length; i++) {
			if(aa[i].annotationType().equals(annotationClass)) {
				return (A) aa[i];
			}
		}
		return null;
    }

	/**
	 *
	 *  @com.intel.drl.spec_ref
	 * 
	 **/
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		return getAnnotation(annotationClass) != null;
    }
    
    /**
     * The defining loader.
     */
    private final ClassLoader loader;
    
    /**
     * A map of {url<String>, attrs<Manifest>} pairs for caching 
     * attributes of bootsrap jars.
     */
    private static SoftReference<Map<String, Manifest>> jarCache;

    /**
     * An url of a source jar, for deffered attributes initialization.
     * After the initialization, if any, is reset to null.
     */
    private String jar;  

    private String implTitle;

    private String implVendor;

    private String implVersion;

    private final String name;

    private URL sealBase;

    private String specTitle;

    private String specVendor;

    private String specVersion;

    /**
     * Name must not be null.
     */
    Package(ClassLoader ld, String packageName, String sTitle, String sVersion, String sVendor,
            String iTitle, String iVersion, String iVendor, URL base) {
        loader = ld;
        name = packageName;
        specTitle = sTitle;
        specVersion = sVersion;
        specVendor = sVendor;
        implTitle = iTitle;
        implVersion = iVersion;
        implVendor = iVendor;
        sealBase = base;
    }
    
    /**
     * Lazy initialization constructor; this Package instance will try to 
     * resolve optional attributes only if such value is requested. 
     * Name must not be null.
     */
    Package(ClassLoader ld, String packageName, String jar) {
        loader = ld;
        name = packageName;
        this.jar = jar;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static Package getPackage(String name) {
        ClassLoader callerLoader = VMClassRegistry.getClassLoader(VMStack
                .getCallerClass(0));
        return callerLoader == null ? ClassLoader.BootstrapLoader
                .getPackage(name) : callerLoader.getPackage(name);
        }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static Package[] getPackages() {
        ClassLoader callerLoader = VMClassRegistry.getClassLoader(VMStack
                .getCallerClass(0));
        if (callerLoader == null) {
            Collection<Package> pkgs = ClassLoader.BootstrapLoader.getPackages();
            return (Package[]) pkgs.toArray(new Package[pkgs.size()]);
        }
        return callerLoader.getPackages();
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public String getImplementationTitle() {
        if (jar != null) {
            init();
        }
        return implTitle;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public String getImplementationVendor() {
        if (jar != null) {
            init();
        }
        return implVendor;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public String getImplementationVersion() {
        if (jar != null) {
            init();
        }
        return implVersion;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public String getName() {
        return name;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public String getSpecificationTitle() {
        if (jar != null) {
            init();
        }
        return specTitle;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public String getSpecificationVendor() {
        if (jar != null) {
            init();
        }
        return specVendor;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public String getSpecificationVersion() {
        if (jar != null) {
            init();
        }
        return specVersion;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public boolean isCompatibleWith(String desiredVersion)
            throws NumberFormatException {

        if (jar != null) {
            init();
        }

        if (specVersion == null || specVersion.length() == 0) { 
            throw new NumberFormatException(
                    "No specification version defined for the package");
        }

        if (!specVersion.matches("[\\p{javaDigit}]+(.[\\p{javaDigit}]+)*")) {
            throw new NumberFormatException(
                    "Package specification version is not of the correct dotted form : " 
                    + specVersion);
        }
        
        if (desiredVersion == null || desiredVersion.length() == 0) {
            throw new NumberFormatException("Empty version to check");
        }

        if (!desiredVersion.matches("[\\p{javaDigit}]+(.[\\p{javaDigit}]+)*")) {
            throw new NumberFormatException(
                    "Desired version is not of the correct dotted form : " 
                    + desiredVersion);
        }
        
        StringTokenizer specVersionTokens = new StringTokenizer(specVersion,
                ".");
        
        StringTokenizer desiredVersionTokens = new StringTokenizer(
                desiredVersion, ".");

        try {
            while (specVersionTokens.hasMoreElements()) {
                int desiredVer = Integer.parseInt(desiredVersionTokens
                        .nextToken());
                int specVer = Integer.parseInt(specVersionTokens.nextToken());
                if (specVer != desiredVer) {
                    return specVer > desiredVer;
                }
            }
        } catch (NoSuchElementException e) {
           /*
            * run out of tokens for desiredVersion
            */
        }
        
        /*
         *   now, if desired is longer than spec, and they have been 
         *   equal so far (ex.  1.4  <->  1.4.0.0) then the remainder
         *   better be zeros
         */

    	while (desiredVersionTokens.hasMoreTokens()) {
    		if (0 != Integer.parseInt(desiredVersionTokens.nextToken())) {
        		return false;
        	}
    	}
        
        return true;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public boolean isSealed() {
        if (jar != null) {
            init();
        }
        return sealBase != null;
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public boolean isSealed(URL url) {
        if (jar != null) {
            init();
        }
        return url.equals(sealBase);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public String toString() {
        if (jar != null) {
            init();
        }
        return "package " + name + (specTitle != null ? " " + specTitle : "")
                + (specVersion != null ? " " + specVersion : "");
    }
    
    /**
     * Performs initialization of optional attributes, if the source jar location 
     * was specified in the lazy constructor.
     */
    private void init() {
        try {
            Map<String, Manifest> map = null;
            Manifest manifest = null;
            URL sealURL = null;
            if (jarCache != null && (map = jarCache.get()) != null) {
                manifest = map.get(jar); 
            }
            
            if (manifest == null) {
                final URL url = sealURL = new URL(jar);
                            
                manifest = AccessController.doPrivileged(
                    new PrivilegedAction<Manifest>() {
                        public Manifest run()
                        {
                            try {
                                return ((JarURLConnection)url
                                        .openConnection()).getManifest();
                            } catch (Exception e) {
                                return new Manifest();
                            }
                        }
                    });
                if (map == null) {
                    map = new Hashtable<String, Manifest>();
                    if (jarCache == null) {
                        jarCache = new SoftReference<Map<String, Manifest>>(map);
                    }
                }
                map.put(jar, manifest);
            }

            Attributes mainAttrs = manifest.getMainAttributes();
            Attributes pkgAttrs = manifest.getAttributes(name.replace('.','/')+"/");
            
            specTitle = pkgAttrs == null || (specTitle = pkgAttrs
                    .getValue(Attributes.Name.SPECIFICATION_TITLE)) == null
                    ? mainAttrs.getValue(Attributes.Name.SPECIFICATION_TITLE)
                    : specTitle;
            specVersion = pkgAttrs == null || (specVersion = pkgAttrs
                    .getValue(Attributes.Name.SPECIFICATION_VERSION)) == null
                    ? mainAttrs.getValue(Attributes.Name.SPECIFICATION_VERSION)
                    : specVersion;
            specVendor = pkgAttrs == null || (specVendor = pkgAttrs
                    .getValue(Attributes.Name.SPECIFICATION_VENDOR)) == null
                    ? mainAttrs.getValue(Attributes.Name.SPECIFICATION_VENDOR)
                    : specVendor;
            implTitle = pkgAttrs == null || (implTitle = pkgAttrs
                    .getValue(Attributes.Name.IMPLEMENTATION_TITLE)) == null
                    ? mainAttrs.getValue(Attributes.Name.IMPLEMENTATION_TITLE)
                    : implTitle;
            implVersion = pkgAttrs == null || (implVersion = pkgAttrs
                    .getValue(Attributes.Name.IMPLEMENTATION_VERSION)) == null
                    ? mainAttrs
                        .getValue(Attributes.Name.IMPLEMENTATION_VERSION)
                    : implVersion;
            implVendor = pkgAttrs == null || (implVendor = pkgAttrs
                    .getValue(Attributes.Name.IMPLEMENTATION_VENDOR)) == null
                    ? mainAttrs.getValue(Attributes.Name.IMPLEMENTATION_VENDOR)
                    : implVendor;
            String sealed = pkgAttrs == null || (sealed = pkgAttrs
                    .getValue(Attributes.Name.SEALED)) == null ? mainAttrs
                    .getValue(Attributes.Name.SEALED) : sealed;
            if (Boolean.valueOf(sealed).booleanValue()) {
                sealBase = sealURL != null ? sealURL : new URL(jar); 
            }
        } catch (Exception e) {}
        jar = null;
    }
}
