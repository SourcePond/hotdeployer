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
package ch.sourcepond.io.hotdeployer.impl.observer;

import ch.sourcepond.io.fileobserver.api.SimpleDispatchRestriction;
import org.osgi.framework.Bundle;
import org.osgi.framework.VersionRange;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

/**
 *
 */
public class BundlePathDeterminator {
    /*
     *
     */
    static final String SYMBOLIC_NAME_PATTERN = "regex:^${prefix}[\\w\\.-]*";

    /*
     * ^(\d+(\.\d+(\.\d+(\.[\d\w-]+)?)?)?)$|^([\[\(]\d+(\.\d+(\.\d+(\.[\d\w-]+)?)?)?(,\s*)?(\d+(\.\d+(\.\d+(\.[\d\w-]+)?)?)?)?(\]|\)))$
     */
    static final String VERSION_RANGE_PATTERN = "regex:^(\\d+(\\.\\d+(\\.\\d+(\\.[\\d\\w-]+)?)?)?)$|^" +
            "([\\[\\(]\\d+(\\.\\d+(\\.\\d+(\\.[\\d\\w-]+)?)?)?(,\\s*)?(\\d+(\\.\\d+(\\.\\d+(\\.[\\d\\w-]+)?)?)?)?" +
            "(\\]|\\)))$";

    private static final char[] TO_BE_ESCAPED = {'\\', '^', '$', '.', '|', '?', '*', '+', '(', ')', '[', '{'};
    private final VersionRangeFactory versionRangeFactory;
    private volatile PathMatcher symbolicNameMatcher;
    private volatile PathMatcher versionRangeMatcher;

    // Constructor for activator
    public BundlePathDeterminator() {
        this(new VersionRangeFactory());
    }

    // Constructor for testing
    BundlePathDeterminator(final VersionRangeFactory pVersionRangeFactory) {
        versionRangeFactory = pVersionRangeFactory;
    }

    static String escape(final String pPrefix) {
        final StringBuilder builder = new StringBuilder(pPrefix);
        for (final char toBeEscaped : TO_BE_ESCAPED) {
            for (int i = 0; i < builder.length(); i++) {
                if (builder.charAt(i) == toBeEscaped) {
                    builder.insert(i++, '\\');
                }
            }
        }
        return builder.toString();
    }

    DispatchRestrictionProxy createProxy(final SimpleDispatchRestriction pDelegate, final Bundle pBundle) {
        return new DispatchRestrictionProxy(this, pDelegate, pBundle);
    }

    BundlePathMatcher create(final PathMatcher pMatcher, final Bundle pBundle) {
        return new BundlePathMatcher(this, pBundle, pMatcher);
    }

    public void setConfig(final FileSystem pFs, final String pPrefix) {
        symbolicNameMatcher = pFs.getPathMatcher(SYMBOLIC_NAME_PATTERN.replace("${prefix}", escape(pPrefix)));
        versionRangeMatcher = pFs.getPathMatcher(VERSION_RANGE_PATTERN);
    }

    boolean apply(final PathMatcher pMatcher, final Bundle pBundle, final Path pPath) {
        final int endIndex = pPath.getNameCount();
        Path versionRangePart = null;
        boolean matches = endIndex > 2 &&
                symbolicNameMatcher.matches(pPath.subpath(0, 1)) &&
                versionRangeMatcher.matches((versionRangePart = pPath.subpath(1, 2))) &&
                pMatcher.matches(pPath.subpath(2, endIndex));
        if (matches) {
            final VersionRange range = versionRangeFactory.createVersion(versionRangePart.toString());
            matches = range.includes(pBundle.getVersion());
        }
        return matches;
    }
}
