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

import org.osgi.framework.VersionRange;

import java.nio.file.Path;

import static java.lang.String.format;

/**
 *
 */
public class BundleNotAvailableException extends RuntimeException {
    private final String symbolicName;
    private final VersionRange versionRange;

    /**
     *
     */
    public BundleNotAvailableException(final Path pBundleKey, final String pSymbolicName, final VersionRange pVersionRange) {
        super(format("No bundle found with symbolic-name %s and version %s!\n" +
                        "The bundle may have been uninstalled. If you still need its resources, move them into\n" +
                        "a top-level directory because %s is not valid anymore, or, re-install the bundle.",
                pSymbolicName, pVersionRange, pBundleKey));
        symbolicName = pSymbolicName;
        versionRange = pVersionRange;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public VersionRange getVersionRange() {
        return versionRange;
    }

    public String toString() {
        return "symbolic-Name: " + symbolicName + ", version-range: " + versionRange;
    }
}
