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
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

import java.nio.file.PathMatcher;

import static org.mockito.Mockito.*;

/**
 *
 */
public class DispatchRestrictionProxyTest {
    private static final String ANY_SYNTAX_AND_PATTERN = "glob:anyPattern";
    private final SimpleDispatchRestriction restriction = mock(SimpleDispatchRestriction.class);
    private final BundlePathDeterminator determinator = mock(BundlePathDeterminator.class);
    private final PathMatcher baseMatcher = mock(PathMatcher.class);
    private final BundlePathMatcher bundlePathMatcher = mock(BundlePathMatcher.class);
    private final Bundle bundle = mock(Bundle.class);
    private final DispatchRestrictionProxy proxy = new DispatchRestrictionProxy(determinator, restriction, bundle);

    @Before
    public void setup() {
        when(restriction.addPathMatcher(ANY_SYNTAX_AND_PATTERN)).thenReturn(baseMatcher);
        when(determinator.create(baseMatcher, bundle)).thenReturn(bundlePathMatcher);
    }

    @Test
    public void addPathMatcherWithSyntaxAndPattern() {
        proxy.addPathMatcher(ANY_SYNTAX_AND_PATTERN);
        verify(restriction).addPathMatcher(bundlePathMatcher);
    }

    @Test
    public void addCustomPathMatcher() {
        proxy.addPathMatcher(baseMatcher);
        verify(restriction).addPathMatcher(bundlePathMatcher);
    }
}
