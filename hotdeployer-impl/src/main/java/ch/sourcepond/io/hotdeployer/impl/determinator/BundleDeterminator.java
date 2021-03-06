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
package ch.sourcepond.io.hotdeployer.impl.determinator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.VersionRange;

import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;

/**
 *
 */
public class BundleDeterminator {
    public static final int SPECIAL_BUNDLE_NAME_COUNT = 2;
    private static final int SYMBOLIC_NAME_INDEX = 0;
    private static final int VERSION_INDEX = 1;
    private static final int WHOLE_BUNDLE_DIRECTORY = 1;
    private static final int VERSION_DIRECTORY_ONLY = 2;
    private final ConcurrentMap<Path, Bundle> bundles = new ConcurrentHashMap<>();
    private final BundleContext context;
    private final Bundle systemBundle;
    private volatile String prefix;

    BundleDeterminator(final BundleContext pContext) {
        context = pContext;
        systemBundle = pContext.getBundle(SYSTEM_BUNDLE_ID);
    }

    public void setPrefix(final String pPrefix) {
        prefix = pPrefix;
    }

    private boolean isBoundToBundle(final Path pRelativePath) {
        return pRelativePath.getName(SYMBOLIC_NAME_INDEX).toString().startsWith(prefix);
    }

    private Path toVersionDirectory(final Path pRelativePath) {
        return pRelativePath.subpath(SYMBOLIC_NAME_INDEX, SPECIAL_BUNDLE_NAME_COUNT);
    }

    public Bundle determine(final Path pRelativePath) throws BundleDeterminationException {
        final Bundle bundle;
        if (pRelativePath.getNameCount() >= SPECIAL_BUNDLE_NAME_COUNT && isBoundToBundle(pRelativePath)) {
            try {
                bundle = bundles.computeIfAbsent(toVersionDirectory(pRelativePath), this::findBundle);
            } catch (final IllegalArgumentException | NoSuchElementException e) {
                throw new BundleDeterminationException(e);
            }
        } else {
            // If path does not meet the criteria we use the
            bundle = systemBundle;
        }
        return bundle;
    }

    private Bundle findBundle(final Path pBundleKey) {
        Bundle bundle = null;
        final String symbolicName = pBundleKey.getName(SYMBOLIC_NAME_INDEX).toString().substring(prefix.length());
        final VersionRange versionRange = VersionRange.valueOf(pBundleKey.getName(VERSION_INDEX).toString());

        for (final Bundle current : context.getBundles()) {
            if (symbolicName.equals(current.getSymbolicName()) &&
                    versionRange.includes(current.getVersion()) &&
                    (bundle == null || current.getVersion().compareTo(bundle.getVersion()) > 0)) {
                bundle = current;
            }
        }

        if (bundle == null) {
            throw new BundleNotAvailableException(pBundleKey, symbolicName, versionRange);
        }

        return bundle;
    }

    public void clearCacheFor(final Path pRelativePath) {
        if (isBoundToBundle(pRelativePath)) {
            if (WHOLE_BUNDLE_DIRECTORY == pRelativePath.getNameCount()) {
                bundles.keySet().removeIf(p -> p.startsWith(pRelativePath));
            } else if (VERSION_DIRECTORY_ONLY == pRelativePath.getNameCount()) {
                bundles.remove(toVersionDirectory(pRelativePath));
            }
        }
    }
}
