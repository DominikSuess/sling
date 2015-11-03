/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.installer.blacklist.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.FileSystem;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.stream.FileImageInputStream;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.installer.api.tasks.BundleBlacklist;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.service.component.ComponentContext;

@Component(
        immediate = true,
        metatype = true,
        label = "Sling Installer Default Bundle Blacklist",
        description = "This is the default bundle blacklist for the Sling installer. It can utilize" +
            "the bootstrap uninstall directives as blacklist and allows manual addition of blacklist rules.")
@Service(value = BundleBlacklist.class)
public class DefaultBundleBlackList implements BundleBlacklist {

    /**
     * The name of the bootstrap commands file
     */
    public static final String BOOTSTRAP_FILENAME = "sling_bootstrap.txt";

    /**
     * Prefix for uninstalls in command files
     */
    private static String UNINSTALL_PREFIX = "uninstall ";

    @Property(
            label = "Use Boostrapfile",
            description = "Use uninstall commands from bootstrap file as blacklist",
            boolValue = true)
    protected static final String PROP_USE_BOOTSTRAP_FILE = "useBoostrapFile";

    @Property(
            label = "Bundle Blacklist",
            description = "Define blacklisted bundles via '<bundlesymbolicname>[ <versionrange>] (can override single bootstrap directives)'",
            unbounded = PropertyUnbounded.ARRAY)
    protected static final String PROP_BUNDLE_BLACKLIST = "bundleBlackList";

    @Reference
    protected SlingSettingsService slingSettingsService;

    private Map<String, VersionRange> blacklistMap = new HashMap<String, VersionRange>();

    @Activate
    protected void activate(ComponentContext cc) {
        @SuppressWarnings("unchecked")
        Dictionary<String, Object> props = cc.getProperties();

        if (PropertiesUtil.toBoolean(props.get(PROP_USE_BOOTSTRAP_FILE), true)) {
            getBlackListFromBootstrapFile(cc.getBundleContext());
        }
        getBundleBlackListConf(props);

    }

    private void getBlackListFromBootstrapFile(BundleContext bc) {
        BufferedReader r = null;
        try {
            String launchpadFolder = bc.getProperty("sling.launchpad");
            r = new BufferedReader(new FileReader(
                launchpadFolder + File.separator + BOOTSTRAP_FILENAME));
            String line = null;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                String bundleSymbolicName = null;
                VersionRange versionRange = null;
                if (line.length() > 0 && line.startsWith(UNINSTALL_PREFIX)) {
                    final String[] s = line.split(" ");
                    if (s.length >= 2) {
                        bundleSymbolicName = s[1].trim();
                    }
                    if (s.length == 3) {
                        versionRange = new VersionRange(s[2].trim());
                    }
                    blacklistMap.put(bundleSymbolicName, versionRange);
                }
            }
        } catch (IOException ignore) {
            // ignore
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    private void getBundleBlackListConf(Dictionary<String, Object> props) {
        String[] blacklistconf = PropertiesUtil.toStringArray(props.get(PROP_BUNDLE_BLACKLIST), new String[0]);
        for (String bli : blacklistconf) {
            String bundleSymbolicName = null;
            VersionRange versionRange = null;
            final String[] s = bli.split(" ");
            if (s.length >= 1) {
                bundleSymbolicName = s[0].trim();
            }
            if (s.length == 2) {
                versionRange = new VersionRange(s[1].trim());
            }
            blacklistMap.put(bundleSymbolicName, versionRange);
        }
    }

    @Override
    public boolean isBlacklisted(String symbolicName, Version version) {
        if (blacklistMap.containsKey(symbolicName)) {
            VersionRange range = blacklistMap.get(symbolicName);
            return (range == null) || range.includes(version);
        }
        return false;
    }

}
