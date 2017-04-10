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

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Version;

import java.nio.file.Path;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;
import static org.osgi.framework.Version.valueOf;

/**
 *
 */
public class BundleDeterminatorTest {
    private static final String ANY_PREFIX = "anyPrefix_";
    private static final String ANY_SYMBOLIC_NAME = "anySymbolicName";
    private static final String ANY_VERSION = "1.2.3";
    private final Version version = valueOf(ANY_VERSION);
    private final BundleContext context = mock(BundleContext.class);
    private final Bundle userBundle = mock(Bundle.class);
    private final Bundle systemBundle = mock(Bundle.class);
    private final Bundle[] bundles = new Bundle[] { systemBundle, userBundle };
    private final Path relativePath = mock(Path.class);
    private final Path symbolicNamePart = mock(Path.class, withSettings().name(ANY_PREFIX + ANY_SYMBOLIC_NAME));
    private final Path versionNamePart = mock(Path.class, withSettings().name(ANY_VERSION));
    private BundleDeterminator determinator;

    @Before
    public void setup() {
        when(context.getBundle(SYSTEM_BUNDLE_ID)).thenReturn(systemBundle);
        when(userBundle.getSymbolicName()).thenReturn(ANY_SYMBOLIC_NAME);
        when(userBundle.getVersion()).thenReturn(version);
        when(context.getBundles()).thenReturn(bundles);
        when(relativePath.getNameCount()).thenReturn(2);
        when(relativePath.getName(0)).thenReturn(symbolicNamePart);
        when(relativePath.getName(1)).thenReturn(versionNamePart);
        when(relativePath.subpath(0, 2)).thenReturn(relativePath);
        determinator = new BundleDeterminator(context, ANY_PREFIX);
    }

    @Test
    public void determinePathToShort() {
        when(relativePath.getNameCount()).thenReturn(1);
        assertSame(systemBundle, determinator.determine(relativePath));
    }

    @Test
    public void determinePathDoesNotStartWithPrefix() {
        determinator = new BundleDeterminator(context, "someDifferentPrefix");
        assertSame(systemBundle, determinator.determine(relativePath));
    }

    @Test
    public void determineNoAppropriateUserBundleFound() {
        when(userBundle.getSymbolicName()).thenReturn("someDifferentSymbolicName");
        assertSame(systemBundle, determinator.determine(relativePath));
    }

    @Test
    public void determineVersionNotParsable() {
        when(relativePath.getName(1)).thenReturn(mock(Path.class));
        assertSame(systemBundle, determinator.determine(relativePath));
    }

    @Test
    public void determine() {
        assertSame(userBundle, determinator.determine(relativePath));

        // Should have been called exactly once
        verify(context).getBundles();
    }

    @Test
    public void bundleStopped() {
        determinator.determine(relativePath);
        determinator.bundleChanged(new BundleEvent(BundleEvent.STOPPED, userBundle));
        determinator.determine(relativePath);

        // Should have been called exactly twice
        verify(context, times(2)).getBundles();
    }
}
