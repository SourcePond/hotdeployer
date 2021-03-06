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
package ch.sourcepond.io.hotdeployer.impl.listener;

import ch.sourcepond.io.fileobserver.api.SimpleDispatchRestriction;

import java.nio.file.PathMatcher;

/**
 *
 */
class DispatchRestrictionProxy implements SimpleDispatchRestriction {
    private final SimpleDispatchRestriction delegate;
    private final BundlePathDeterminator determinator;

    DispatchRestrictionProxy(final BundlePathDeterminator pDeterminator,
                             final SimpleDispatchRestriction pDelegate) {
        determinator = pDeterminator;
        delegate = pDelegate;
    }

    @Override
    public PathMatcher addPathMatcher(final String pSyntaxAndPattern) {
        return addPathMatcher(delegate.addPathMatcher(pSyntaxAndPattern));
    }

    @Override
    public PathMatcher addPathMatcher(final PathMatcher pCustomMatcher) {
        return delegate.addPathMatcher(determinator.create(pCustomMatcher));
    }
}
