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

import java.nio.file.Path;
import java.nio.file.PathMatcher;

/**
 *
 */
class BundlePathMatcher implements PathMatcher {
    private final PathMatcher matcher;
    private final BundlePathDeterminator determinator;

    BundlePathMatcher(final PathMatcher pMatcher, final BundlePathDeterminator pDeterminator) {
        matcher = pMatcher;
        determinator = pDeterminator;
    }

    @Override
    public boolean matches(final Path path) {
        return determinator.apply(matcher, path);
    }
}
