/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.catalog;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * To get the version of this catalog.
 */
public class VersionHelper {

    private static volatile String version;

    public synchronized String getVersion() {
        if (version != null) {
            return version;
        }
        InputStream is = null;
        // try to load from maven properties first
        try {
            Properties p = new Properties();
            is = getClass().getResourceAsStream("/META-INF/maven/org.apache.camel/camel-catalog/pom.properties");
            if (is != null) {
                p.load(is);
                version = p.getProperty("version", "");
            }
        } catch (Exception e) {
            // ignore
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        // fallback to using Java API
        if (version == null) {
            Package aPackage = getClass().getPackage();
            if (aPackage != null) {
                version = aPackage.getImplementationVersion();
                if (version == null) {
                    version = aPackage.getSpecificationVersion();
                }
            }
        }

        // read the implementation version from the manifest file (useful before packaging)
        if (version == null) {
            try {
                Enumeration<URL> manifestURLs = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
                while (manifestURLs.hasMoreElements()) {
                    URL manifestURL = manifestURLs.nextElement();
                    try (InputStream in = manifestURL.openStream()) {
                        Manifest manifest = new Manifest(in);
                        Attributes attr = manifest.getMainAttributes();
                        String module = attr.getValue("Bundle-Name");

                        if ("camel-catalog".equals(module)) {
                            String version = attr.getValue("Bundle-Version");
                            if (version != null) {
                                this.version = version.trim();
                                break;
                            }
                        }
                    }
                }
            } catch(Exception e) {
                // suppress
            }
        }

        if (version == null) {
            // we could not compute the version so use a blank
            version = "";
        }

        return version;
    }

}
