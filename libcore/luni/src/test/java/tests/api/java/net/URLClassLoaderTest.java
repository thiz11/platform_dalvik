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

package tests.api.java.net;

import dalvik.annotation.BrokenTest; 
import dalvik.annotation.TestTargetClass; 
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.Manifest;

import org.apache.harmony.luni.util.InvalidJarIndexException;
import org.apache.harmony.security.tests.support.TestCertUtils;

import tests.support.Support_Configuration;
import tests.support.resource.Support_Resources;

@TestTargetClass(URLClassLoader.class) 
public class URLClassLoaderTest extends junit.framework.TestCase {

    class BogusClassLoader extends ClassLoader {
        public URL getResource(String res) {
            try {
                return new URL("http://test/BogusClassLoader");
            } catch (MalformedURLException e) {
                return null;
            }
        }
    }

    public class URLClassLoaderExt extends URLClassLoader {

        public URLClassLoaderExt(URL[] urls) {
            super(urls);
        }

        public Class<?> findClass(String cl) throws ClassNotFoundException {
            return super.findClass(cl);
        }
    }
    
    URLClassLoader ucl;
    SecurityManager sm = new SecurityManager() {

        public void checkPermission(Permission perm) {
        }
        
        public void checkCreateClassLoader() {
            throw new SecurityException();
        }
    };

    /**
     * @tests java.net.URLClassLoader#URLClassLoader(java.net.URL[])
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "URLClassLoader",
        args = {java.net.URL[].class}
    )
    public void test_Constructor$Ljava_net_URL() throws MalformedURLException {
        URL[] u = new URL[0];
        ucl = new URLClassLoader(u);
        assertTrue("Failed to set parent", ucl != null
                && ucl.getParent() == URLClassLoader.getSystemClassLoader());

        
        URL [] urls = {new URL("http://foo.com/foo"), 
                       new URL("jar:file://foo.jar!/foo.c"), 
                       new URL("ftp://foo1/foo2/foo.c")};
        
        URLClassLoader ucl1 = new URLClassLoader(urls);
        assertTrue(urls.length == ucl1.getURLs().length);
        
        try {
            Class.forName("test", false, ucl);
            fail("Should throw ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // expected
        }
       
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            new URLClassLoader(u);
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
        
        try {
            new URLClassLoader(new URL[] { null });
        } catch(Exception e) {
            fail("Unexpected exception was thrown: " + e.getMessage());
        }
    }

    /**
     * @tests java.net.URLClassLoader#URLClassLoader(java.net.URL[],
     *        java.lang.ClassLoader)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "URLClassLoader",
        args = {java.net.URL[].class, java.lang.ClassLoader.class}
    )
    public void test_Constructor$Ljava_net_URLLjava_lang_ClassLoader() {
        ClassLoader cl = new BogusClassLoader();
        URL[] u = new URL[0];
        ucl = new URLClassLoader(u, cl);
        URL res = ucl.getResource("J");
        assertNotNull(res);
        assertEquals("Failed to set parent", "/BogusClassLoader", res.getFile());
        
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            new URLClassLoader(u, cl);
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    /**
     * @tests java.net.URLClassLoader#findResources(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "IOException checking missed.",
        method = "findResources",
        args = {java.lang.String.class}
    )
    @BrokenTest("web address used from support doesn't work anymore")
    public void test_findResourcesLjava_lang_String() throws IOException {
        Enumeration res = null;
        String[] resValues = { "This is a test resource file.",
                "This is a resource from a subdir" };

        URL[] urls = new URL[2];
        urls[0] = new URL(Support_Resources.getResourceURL("/"));
        urls[1] = new URL(Support_Resources.getResourceURL("/subdir1/"));
        ucl = new URLClassLoader(urls);
        res = ucl.findResources("RESOURCE.TXT");
        assertNotNull("Failed to locate resources", res);

        int i = 0;
        while (res.hasMoreElements()) {
            StringBuffer sb = new StringBuffer();
            InputStream is = ((URL) res.nextElement()).openStream();
            int c;
            while ((c = is.read()) != -1) {
                sb.append((char) c);
            }
            assertEquals("Returned incorrect resource/or in wrong order",
                    resValues[i++], sb.toString());
        }
        assertTrue("Incorrect number of resources returned: " + i, i == 2);
    }

    /**
     * @tests java.net.URLClassLoader#getURLs()
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getURLs",
        args = {}
    )
    public void test_getURLs() throws MalformedURLException {
        URL[] urls = new URL[4];
        urls[0] = new URL("http://" + Support_Configuration.HomeAddress);
        urls[1] = new URL("http://" + Support_Configuration.TestResources + "/");
        urls[2] = new URL("ftp://" + Support_Configuration.TestResources + "/");
        urls[3] = new URL("jar:file:c://" + Support_Configuration.TestResources
                + "!/");
        ucl = new URLClassLoader(urls);
        URL[] ucUrls = ucl.getURLs();
        for (int i = 0; i < urls.length; i++) {
            assertEquals("Returned incorrect URL[]", urls[i], ucUrls[i]);
        }
    }

    /**
     * @tests java.net.URLClassLoader#newInstance(java.net.URL[])
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "newInstance",
        args = {java.net.URL[].class}
    )
    @BrokenTest("web address used from support doesn't work anymore")
    public void test_newInstance$Ljava_net_URL() throws MalformedURLException,
            ClassNotFoundException {
        // Verify that loaded class' have correct permissions
        Class cl = null;
        URL res = null;
        URL[] urls = new URL[1];
        urls[0] = new URL(Support_Resources.getResourceURL("/UCL/UCL.jar"));
        ucl = URLClassLoader.newInstance(urls);
        cl = ucl.loadClass("ucl.ResClass");

        res = cl.getClassLoader().getResource("XX.class");
        assertNotNull("Failed to load class", cl);
        assertNotNull(
                "Loaded class unable to access resource from same codeSource",
                res);
        cl = null;

        urls[0] = new URL("jar:"
                + Support_Resources.getResourceURL("/UCL/UCL.jar!/"));
        ucl = URLClassLoader.newInstance(urls);
        cl = ucl.loadClass("ucl.ResClass");
        assertNotNull("Failed to load class from explicit jar URL", cl);
    }

    /**
     * @tests java.net.URLClassLoader#newInstance(java.net.URL[],
     *        java.lang.ClassLoader)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "newInstance",
        args = {java.net.URL[].class, java.lang.ClassLoader.class}
    )
    public void test_newInstance$Ljava_net_URLLjava_lang_ClassLoader() {
        ClassLoader cl = new BogusClassLoader();
        URL[] u = new URL[0];
        ucl = URLClassLoader.newInstance(u, cl);
        URL res = ucl.getResource("J");
        assertNotNull(res);
        assertEquals("Failed to set parent", "/BogusClassLoader", res.getFile());
    }

    /**
     * @tests java.net.URLClassLoader#URLClassLoader(java.net.URL[],
     *        java.lang.ClassLoader, java.net.URLStreamHandlerFactory)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "URLClassLoader",
        args = {java.net.URL[].class, java.lang.ClassLoader.class, 
                java.net.URLStreamHandlerFactory.class}
    )
    public void test_Constructor$Ljava_net_URLLjava_lang_ClassLoaderLjava_net_URLStreamHandlerFactory() {
        class TestFactory implements URLStreamHandlerFactory {
            public URLStreamHandler createURLStreamHandler(String protocol) {
                return null;
            }
        }
        ClassLoader cl = new BogusClassLoader();
        URL[] u = new URL[0];
        ucl = new URLClassLoader(u, cl, new TestFactory());
        URL res = ucl.getResource("J");
        assertNotNull(res);
        assertEquals("Failed to set parent", "/BogusClassLoader", res.getFile());
        
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            new URLClassLoader(u, cl, new TestFactory());
            fail("SecurityException should be thrown.");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "addURL",
        args = { URL.class }
    )
    public void test_addURLLjava_net_URL() throws MalformedURLException {
        URL[] u = new URL[0];
        
        URL [] urls = {new URL("http://foo.com/foo"), 
                       new URL("jar:file://foo.jar!/foo.c"), 
                       new URL("ftp://foo1/foo2/foo.c"), null};
        
        TestURLClassLoader tucl = new TestURLClassLoader(u);
        
        for(int i = 0; i < urls.length;) {
            tucl.addURL(urls[i]);
            i++;
            URL [] result = tucl.getURLs();
            assertEquals("Result array length is incorrect: " + i, 
                                                            i, result.length);
            for(int j = 0; j < result.length; j++) {
                assertEquals("Result array item is incorrect: " + j, 
                                                            urls[j], result[j]);
            }
        }
    }
    
    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        notes = "",
        method = "getPermissions",
        args = { CodeSource.class }
    )    
    public void test_getPermissions() throws MalformedURLException {
        URL url = new URL("http://" + Support_Configuration.SpecialInetTestAddress);
        Certificate[] chain = TestCertUtils.getCertChain();
        CodeSource cs = new CodeSource(url, chain);
        TestURLClassLoader cl = new TestURLClassLoader(new URL[] {url});
        PermissionCollection permCol = cl.getPermissions(cs);
        assertNotNull(permCol);
        
        URL url1 = new URL("file://foo/foo.c");
        TestURLClassLoader cl1 = new TestURLClassLoader(new URL[] {url});
        CodeSource cs1 = new CodeSource(url1, chain);
        PermissionCollection permCol1 = cl1.getPermissions(cs1);
        assertNotNull(permCol1);
    }
    
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "definePackage",
        args = { java.lang.String.class, java.util.jar.Manifest.class, 
                 java.net.URL.class }
    )
    public void test_definePackage() throws MalformedURLException {
        Manifest manifest = new Manifest();
        URL[] u = new URL[0];
        TestURLClassLoader tucl = new TestURLClassLoader(u);
        
        URL [] urls = {new URL("http://foo.com/foo"), 
                new URL("jar:file://foo.jar!/foo.c"), 
                new URL("ftp://foo1/foo2/foo.c"),
                new URL("file://new/package/name/"),
                null};
        
        String packageName = "new.package.name";
        
        for(int i = 0; i < urls.length; i++) {
            Package pack = tucl.definePackage(packageName + i, manifest, urls[i]);
            assertEquals(packageName + i, pack.getName());
            assertNull("Implementation Title is not null", 
                    pack.getImplementationTitle());
            assertNull("Implementation Vendor is not null", 
                    pack.getImplementationVendor());
            assertNull("Implementation Version is not null.", 
                    pack.getImplementationVersion());
        }
        
        try {
            tucl.definePackage(packageName + "0", manifest, null);
            fail("IllegalArgumentException was not thrown.");
        } catch(IllegalArgumentException iae) {
            //expected
        }
    }
    
    class TestURLClassLoader extends URLClassLoader {
        public TestURLClassLoader(URL[] urls) {
            super(urls);
        }
        
        public void addURL(URL url) {
            super.addURL(url);
        }
        
        public Package definePackage(String name,
                                     Manifest man,
                                     URL url)
                                     throws IllegalArgumentException {
            return super.definePackage(name, man, url);
        }
        
        public Class<?> findClass(String name)
                                        throws ClassNotFoundException {
            return super.findClass(name);
        }
        
        protected PermissionCollection getPermissions(CodeSource codesource) {
            return super.getPermissions(codesource);
        }
    }
    
    /**
     * @throws ClassNotFoundException
     * @throws IOException
     * @tests java.net.URLClassLoader#findClass(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "findClass",
        args = {java.lang.String.class}
    )
    @BrokenTest("")
    public void test_findClassLjava_lang_String()
            throws ClassNotFoundException, IOException {
        File resources = Support_Resources.createTempFolder();
        String resPath = resources.toString();
        if (resPath.charAt(0) == '/' || resPath.charAt(0) == '\\') {
            resPath = resPath.substring(1);
        }

        java.net.URL[] urls = new java.net.URL[1];
        java.net.URLClassLoader ucl = null;
        boolean classFound;
        boolean exception;
        boolean goodException;
        Enumeration en;
        boolean resourcesFound;
        Support_Resources.copyFile(resources, "JarIndex", "hyts_11.jar");
        Support_Resources.copyFile(resources, "JarIndex", "hyts_12.jar");
        Support_Resources.copyFile(resources, "JarIndex", "hyts_13.jar");
        Support_Resources.copyFile(resources, "JarIndex", "hyts_14.jar");
        urls[0] = new URL("file:/" + resPath + "/JarIndex/hyts_11.jar");
        ucl = URLClassLoader.newInstance(urls, null);
        URL resURL = ucl.findResource("Test.txt");
        URL reference = new URL("jar:file:/" + resPath.replace('\\', '/')
                + "/JarIndex/hyts_14.jar!/Test.txt");
        assertTrue("Resource not found: " + resURL + " ref: " + reference,
                resURL.equals(reference));

        Class c = Class.forName("cpack.CNothing", true, ucl);
        assertNotNull(c);

        Support_Resources.copyFile(resources, "JarIndex", "hyts_21.jar");
        Support_Resources.copyFile(resources, "JarIndex", "hyts_22.jar");
        Support_Resources.copyFile(resources, "JarIndex", "hyts_23.jar");
        urls[0] = new URL("file:/" + resPath + "/JarIndex/hyts_21.jar");
        ucl = URLClassLoader.newInstance(urls, null);
        en = ucl.findResources("bpack/");

        try {
            resourcesFound = true;
            URL url1 = (URL) en.nextElement();
            URL url2 = (URL) en.nextElement();
            System.out.println(url1);
            System.out.println(url2);
            resourcesFound = resourcesFound
                    && url1.equals(new URL("jar:file:/"
                            + resPath.replace('\\', '/')
                            + "/JarIndex/hyts_22.jar!/bpack/"));
            resourcesFound = resourcesFound
                    && url2.equals(new URL("jar:file:/"
                            + resPath.replace('\\', '/')
                            + "/JarIndex/hyts_23.jar!/bpack/"));
            if (en.hasMoreElements()) {
                resourcesFound = false;
            }
        } catch (NoSuchElementException e) {
            resourcesFound = false;
        }
        assertTrue("Resources not found (1)", resourcesFound);

        Class c2 = Class.forName("bpack.Homer", true, ucl);
        assertNotNull(c2);

        try {
            Class.forName("bpack.Bart", true, ucl);
            fail("InvalidJarIndexException should be thrown");
        } catch (RuntimeException e) {
            e.printStackTrace();
            // expected
        }

        try {
            Class.forName("Main4", true, ucl);
            fail("ClassNotFoundException should be thrown");
        } catch (ClassNotFoundException e) {
            // Expected
        }

        Support_Resources.copyFile(resources, "JarIndex", "hyts_22-new.jar");
        urls[0] = new URL("file:/" + resPath + "/JarIndex/hyts_22-new.jar");
        ucl = URLClassLoader.newInstance(urls, null);
        assertNotNull("Cannot find resource", ucl.findResource("cpack/"));
        Support_Resources.copyFile(resources, "JarIndex", "hyts_11.jar");
        urls[0] = new URL("file:/" + resPath + "/JarIndex/hyts_31.jar");
        ucl = URLClassLoader.newInstance(urls, null);

        try {
            Class.forName("cpack.Mock", true, ucl);
            fail("ClassNotFoundException should be thrown");
        } catch (ClassNotFoundException e) {
            // Expected
        }

        // testing circular reference
        Support_Resources.copyFile(resources, "JarIndex", "hyts_41.jar");
        Support_Resources.copyFile(resources, "JarIndex", "hyts_42.jar");
        urls[0] = new URL("file:/" + resPath + "/JarIndex/hyts_41.jar");
        ucl = URLClassLoader.newInstance(urls, null);
        en = ucl.findResources("bpack/");
        resourcesFound = resourcesFound
                && ((URL) en.nextElement()).equals(new URL("jar:file:/"
                        + resPath.replace('\\', '/')
                        + "/JarIndex/hyts_42.jar!/bpack/"));
        assertTrue("Resources not found (2)", resourcesFound);
        assertFalse("No more resources expected", en.hasMoreElements());
       
        // Regression test for HARMONY-2357.
        try {
            URLClassLoaderExt cl = new URLClassLoaderExt(new URL[557]);
            cl.findClass("0");
            fail("NullPointerException should be thrown");
        } catch (NullPointerException npe) {
            // Expected
        }

        // Regression test for HARMONY-2871.
        URLClassLoader cl = new URLClassLoader(new URL[] { new URL("file:/foo.jar") });

        try {
            Class.forName("foo.Foo", false, cl);
        } catch (Exception ex) {
            // Don't care
        }

        try {
            Class.forName("foo.Foo", false, cl);
            fail("NullPointerException should be thrown");
        } catch (ClassNotFoundException cnfe) {
            // Expected
        }
    }

    /**
     * @tests java.net.URLClassLoader#findResource(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "findResource",
        args = {java.lang.String.class}
    )
    @BrokenTest("web address used from support doesn't work anymore")
    public void test_findResourceLjava_lang_String()
            throws MalformedURLException {
        URL res = null;

        URL[] urls = new URL[2];
        urls[0] = new URL("http://" + Support_Configuration.HomeAddress);
        urls[1] = new URL(Support_Resources.getResourceURL("/"));
        ucl = new URLClassLoader(urls);
        res = ucl.findResource("RESOURCE.TXT");
        assertNotNull("Failed to locate resource", res);

        StringBuffer sb = new StringBuffer();
        try {
            java.io.InputStream is = res.openStream();

            int c;
            while ((c = is.read()) != -1) {
                sb.append((char) c);
            }
            is.close();
        } catch (IOException e) {
        }
        assertTrue("Returned incorrect resource", !sb.toString().equals(
                "This is a test resource file"));
    }
    
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Checks getResource, indirectly checks findResource",
            method = "getResource",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Checks getResource, indirectly checks findResource",
            method = "findResource",
            args = {java.lang.String.class}
        )
    })
    public void testFindResource_H3461() throws Exception {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File dir = new File(tmpDir, "encode#me");
        File f, f2;
        URLClassLoader loader;
        URL dirUrl;
        
        if (!dir.exists()) {
            dir.mkdir();
        }
        dir.deleteOnExit();
        dirUrl = dir.toURI().toURL();
        loader = new URLClassLoader( new URL[] { dirUrl });

        f = File.createTempFile("temp", ".dat", dir);
        f.deleteOnExit();
        f2 = File.createTempFile("bad#name#", ".dat", dir);
        f2.deleteOnExit();
                
        assertNotNull("Unable to load resource from path with problematic name",
            loader.getResource(f.getName()));
        assertEquals("URL was not correctly encoded",
            f2.toURI().toURL(),    
            loader.getResource(f2.getName()));
    }

    /**
     * @tests java.net.URLClassLoader#getResource(java.lang.String)
     */
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "getResource",
        args = {java.lang.String.class}
    )    
    public void test_getResourceLjava_lang_String()
            throws MalformedURLException {
        URL url1 = new URL("file:///");
        URLClassLoader loader = new URLClassLoader(new URL[] { url1 }, null);
        long start = System.currentTimeMillis();
        // try without the leading /
        URL result = loader.getResource("dir1/file1");
        long end = System.currentTimeMillis();
        long time = end - start;
        if (time < 100) {
            time = 100;
        }

        start = System.currentTimeMillis();
        // try with the leading forward slash
        result = loader.getResource("/dir1/file1");
        end = System.currentTimeMillis();
        long uncTime = end - start;
        assertTrue("too long. UNC path formed? UNC time: " + uncTime
                + " regular time: " + time, uncTime <= (time * 4));
    }
    
    /**
     * Regression for Harmony-2237 
     */
    @TestTargetNew(
        level = TestLevel.PARTIAL,
        notes = "Regression test",
        method = "findResource",
        args = {java.lang.String.class}
    )
    public void test_getResource() throws Exception {        
        URLClassLoader urlLoader = getURLClassLoader();
        assertNull(urlLoader.findResource("XXX")); //$NON-NLS-1$
    }

    private static URLClassLoader getURLClassLoader() {
        String classPath = System.getProperty("java.class.path");
        StringTokenizer tok = new StringTokenizer(classPath, File.pathSeparator);
        Vector<URL> urlVec = new Vector<URL>();
        String resPackage = Support_Resources.RESOURCE_PACKAGE;
        try {
            while (tok.hasMoreTokens()) {
                String path = tok.nextToken();
                String url;
                if (new File(path).isDirectory())
                    url = "file:" + path + resPackage + "subfolder/";
                else
                    url = "jar:file:" + path + "!" + resPackage + "subfolder/";
                urlVec.addElement(new URL(url));
            }
        } catch (MalformedURLException e) {
            // do nothing
        }
        URL[] urls = new URL[urlVec.size()];
        for (int i = 0; i < urlVec.size(); i++) {
            urls[i] = urlVec.elementAt(i);
        }            
        URLClassLoader loader = new URLClassLoader(urls, null);
        return loader;
    }
}