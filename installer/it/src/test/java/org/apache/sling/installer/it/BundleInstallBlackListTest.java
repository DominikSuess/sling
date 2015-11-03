/*
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
package org.apache.sling.installer.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.sling.installer.api.tasks.BundleBlacklist;
import org.apache.sling.installer.core.impl.tasks.BundleInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

@RunWith(PaxExam.class)
public class BundleInstallBlackListTest extends OsgiInstallerTestBase {

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return defaultConfiguration();
    }

    @Before
    public void setUp() {
        setupInstaller();
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testBlacklistBundleVersion() throws Exception {
        final String symbolicName = "osgi-installer-testbundle";

        assertNull("Test bundle must not be present before test", findBundle(symbolicName));

        // configure blacklist bundle to ignore older version
        BundleBlacklist bl = new BundleBlacklist() {

            public boolean isBlacklisted(String symbolicName, Version version) {
                boolean isSameSymbolicName = "osgi-installer-testbundle".equals(symbolicName);
                boolean isBlackListVersion = version.equals(new Version("1.1"));
                return isSameSymbolicName && isBlackListVersion;
            }
        };

        ServiceRegistration<?> sr = bundleContext.registerService(BundleBlacklist.class.getName(), bl, null);

        // Install first test bundle and check version
        long bundleId = 0;
        {
            assertNull("Test bundle must be absent before installing", findBundle(symbolicName));
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")), null);
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                new BundleEvent(symbolicName, "1.0", org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent(symbolicName, "1.0", org.osgi.framework.BundleEvent.STARTED));
            final Bundle b = assertBundle("After installing", symbolicName, "1.0", Bundle.ACTIVE);
            bundleId = b.getBundleId();
        }

        // Try to install blacklisted version of bundle
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar"), "digestA"), null);
            sleep(150);
            this.assertNoBundleEvents("Bundle install of blacklisted version should not cause any change.", listener,
                symbolicName);
            final Bundle b = assertBundle("After updating to 1.1", symbolicName, "1.0", Bundle.ACTIVE);
            assertEquals("Bundle ID must not change after update", bundleId, b.getBundleId());
        }

        sr.unregister();

    }

    @Test
    public void testUninstallWithBlacklistedVersions() throws Exception {
        final String symbolicName = "osgi-installer-testbundle";

        assertNull("Test bundle must not be present before test", findBundle(symbolicName));

        // Install first test bundle and check version
        long bundleId = 0;
        {
            assertNull("Test bundle must be absent before installing", findBundle(symbolicName));
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")), null);
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                new BundleEvent(symbolicName, "1.0", org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent(symbolicName, "1.0", org.osgi.framework.BundleEvent.STARTED));
            final Bundle b = assertBundle("After installing", symbolicName, "1.0", Bundle.ACTIVE);
            bundleId = b.getBundleId();
        }

        // Upgrade to later version, verify
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar"), "digestA"), null);
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                new BundleEvent(symbolicName, "1.0", org.osgi.framework.BundleEvent.STOPPED),
                new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.STARTED));
            final Bundle b = assertBundle("After updating to 1.1", symbolicName, "1.1", Bundle.ACTIVE);
            assertEquals("Bundle ID must not change after update", bundleId, b.getBundleId());
        }

        // configure blacklist bundle to ignore older version
        BundleBlacklist bl = new BundleBlacklist() {

            public boolean isBlacklisted(String symbolicName, Version version) {
                boolean isSameSymbolicName = "osgi-installer-testbundle".equals(symbolicName);
                boolean isBlackListVersion = version.equals(new Version("1.0"));
                return isSameSymbolicName && isBlackListVersion;
            }
        };

        ServiceRegistration<?> sr = bundleContext.registerService(BundleBlacklist.class.getName(), bl, null);

        // Try to uninstall current version and verify uninstall instead of
        // downgrade to blacklisted version
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, null, getNonInstallableResourceUrl(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.STOPPED),
                new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.UNINSTALLED));

            final Bundle b = findBundle(symbolicName);
            assertNull("Testbundle must be gone", b);
        }
        sr.unregister();

    }

    @Test
    public void testUninstallWithBlacklistedIntermediateVersion() throws Exception {
        final String symbolicName = "osgi-installer-testbundle";

        assertNull("Test bundle must not be present before test",
            findBundle(symbolicName));

        // Install first test bundle and check version
        long bundleId = 0;
        {
            assertNull("Test bundle must be absent before installing",
                findBundle(symbolicName));
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")), null);
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                new BundleEvent(symbolicName, "1.0",
                    org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent(symbolicName, "1.0",
                    org.osgi.framework.BundleEvent.STARTED));
            final Bundle b = assertBundle("After installing", symbolicName, "1.0",
                Bundle.ACTIVE);
            bundleId = b.getBundleId();
        }

        // Upgrade to later version, verify
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar"), "digestA"),
                null);
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                new BundleEvent(symbolicName, "1.0",
                    org.osgi.framework.BundleEvent.STOPPED),
                new BundleEvent(symbolicName, "1.1",
                    org.osgi.framework.BundleEvent.STARTED));
            final Bundle b = assertBundle("After updating to 1.1", symbolicName,
                "1.1", Bundle.ACTIVE);
            assertEquals("Bundle ID must not change after update", bundleId,
                b.getBundleId());
        }

        // upgrade to 3rd version
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar"), "digestA"),
                null);
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                new BundleEvent(symbolicName, "1.1",
                    org.osgi.framework.BundleEvent.STOPPED),
                new BundleEvent(symbolicName, "1.2",
                    org.osgi.framework.BundleEvent.STARTED));
            final Bundle b = assertBundle("After updating to 1.2", symbolicName,
                "1.2", Bundle.ACTIVE);
            assertEquals("Bundle ID must not change after update", bundleId,
                b.getBundleId());
        }

        // configure blacklist bundle to ignore 1.1 version
        BundleBlacklist bl = new BundleBlacklist() {

            public boolean isBlacklisted(String symbolicName, Version version) {
                boolean isSameSymbolicName = "osgi-installer-testbundle".equals(symbolicName);
                boolean isBlackListVersion = version.equals(new Version("1.1"));
                return isSameSymbolicName && isBlackListVersion;
            }
        };

        ServiceRegistration<?> sr = bundleContext.registerService(BundleBlacklist.class.getName(), bl,
            null);

        // Try to uninstall current version and verify uninstall instead of
        // downgrade to blacklisted version
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, null, getNonInstallableResourceUrl(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar")));
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                new BundleEvent(symbolicName, "1.2",
                    org.osgi.framework.BundleEvent.STOPPED),
                new BundleEvent(symbolicName, "1.0",
                    org.osgi.framework.BundleEvent.STARTED));

            final Bundle b = assertBundle("After uninstalling 1.2", symbolicName,
                "1.0", Bundle.ACTIVE);
            assertEquals("Bundle ID must not change after update", bundleId,
                b.getBundleId());
        }
        sr.unregister();

    }

}