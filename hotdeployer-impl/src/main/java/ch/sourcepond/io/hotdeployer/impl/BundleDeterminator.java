/*Copyright (C) 2017 Roland Hauser, <sourcepond@gmail.com>
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.io.hotdeployer.impl;

import org.osgi.framework.*;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.osgi.framework.BundleEvent.STOPPED;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;
import static org.osgi.framework.Version.valueOf;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class BundleDeterminator implements BundleListener {
    private static Logger LOG = getLogger(BundleDeterminator.class);
    static final int SPECIAL_BUNDLE_NAME_COUNT = 2;
    private final ConcurrentMap<Path, Bundle> bundles = new ConcurrentHashMap<>();
    private final String prefix;
    private final BundleContext context;
    private final Bundle systemBundle;

    BundleDeterminator(final BundleContext pContext, final String pPrefix) {
        context = pContext;
        prefix = pPrefix;
        systemBundle = pContext.getBundle(SYSTEM_BUNDLE_ID);
    }

    Bundle determine(final Path pRelativePath) {
        final Bundle bundle;
        if (pRelativePath.getNameCount() >= SPECIAL_BUNDLE_NAME_COUNT &&
                pRelativePath.getName(0).toString().startsWith(prefix)) {
            bundle = bundles.computeIfAbsent(
                    pRelativePath.subpath(0, SPECIAL_BUNDLE_NAME_COUNT),
                    this::findBundle);
        } else {
            // If path does not meet the criteria we use the
            bundle = systemBundle;
        }
        return bundle;
    }

    private Bundle findBundle(final Path pBundleKey) {
        Bundle bundle = null;
        try {
            final String symbolicName = pBundleKey.getName(0).toString().substring(prefix.length());
            final Version version = valueOf(pBundleKey.getName(1).toString());

            for (final Bundle current : context.getBundles()) {
                if (symbolicName.equals(current.getSymbolicName()) && version.equals(current.getVersion())) {
                    bundle = current;
                    break;
                }
            }
        } catch (final IllegalArgumentException e) {
            LOG.warn(e.getMessage(), e);
        }

        if (bundle == null) {
            bundle = systemBundle;
        }

        return bundle;
    }

    @Override
    public void bundleChanged(final BundleEvent event) {
        if (STOPPED == event.getType()) {
            bundles.values().remove(event.getBundle());
        }
    }
}
