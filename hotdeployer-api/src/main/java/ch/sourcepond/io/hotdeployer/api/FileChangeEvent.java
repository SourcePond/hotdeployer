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
package ch.sourcepond.io.hotdeployer.api;

import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import org.osgi.framework.Bundle;

/**
 * Extension to the {@link PathChangeEvent} interface which additionally provides access
 * to the source bundle of a change file, see {@link #getSourceBundle()}.
 */
public interface FileChangeEvent extends PathChangeEvent {

    /**
     * Returns the bundle which originally provided the modified resource. The hotdeployer determines the
     * source bundle by analyzing the the first two elements of the relative path (relative to the hotdeployment
     * root) of the changed file. A bundle determination is performed when following criterias are fulfilled:
     * <ul>
     *     <li>The first path element starts with a configured prefix, like __BUNDLE__</li>
     *     <li>The name of the first path element without the prefix matches the requirements of a Bundle-SymbolicName
     *     (allowed characterars: "A-Z a-z 0-9 . _-")</li>
     *     <li>The name of the second path element represents a valid OSGi version.</li>
     * </ul>
     * Examples:
     * <ul>
     *     <li>__BUNDLE__com.foo.bar/1.0.0/...</li>
     *     <li>__BUNDLE__com.foo.bar/1.0.0.RELEASE/...</li>
     *     <li>__BUNDLE__com.foo.bar/1.0.0.20170501-1312/...</li>
     * </ul>
     *
     * It's perfectly valid to arrange files differently. In this case, the bundle determination will be skipped
     * and the system bundle will be returned as source bundle.
     *
     * @return Source bundle of the changed file, never {@code null}
     */
    Bundle getSourceBundle();
}
