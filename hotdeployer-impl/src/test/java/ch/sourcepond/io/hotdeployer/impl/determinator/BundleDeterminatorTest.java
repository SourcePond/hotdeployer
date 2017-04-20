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

import ch.sourcepond.io.hotdeployer.impl.determinator.BundleDeterminationException;
import ch.sourcepond.io.hotdeployer.impl.determinator.BundleDeterminator;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import java.nio.file.Path;
import java.util.NoSuchElementException;

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
    private static final String ANY_VERSION_RANGE = "[1.2,1.3)";
    private static final String ANY_VERSION = "1.2.3";
    private final Version version = valueOf(ANY_VERSION);
    private final BundleContext context = mock(BundleContext.class);
    private final Bundle userBundle = mock(Bundle.class);
    private final Bundle systemBundle = mock(Bundle.class);
    private final Bundle[] bundles = new Bundle[]{systemBundle, userBundle};
    private final Path symbolicNamePart = mock(Path.class, withSettings().name(ANY_PREFIX + ANY_SYMBOLIC_NAME));
    private final Path versionNamePart = mock(Path.class, withSettings().name(ANY_VERSION_RANGE));
    private Path relativePath = mock(Path.class);
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
        determinator = new BundleDeterminator(context);
        determinator.setPrefix(ANY_PREFIX);
    }

    @Test
    public void determinePathToShort() throws Exception {
        when(relativePath.getNameCount()).thenReturn(1);
        assertSame(systemBundle, determinator.determine(relativePath));
    }

    @Test
    public void determinePathDoesNotStartWithPrefix() throws Exception {
        determinator.setPrefix("someDifferentPrefix");
        assertSame(systemBundle, determinator.determine(relativePath));
    }

    @Test
    public void determineNoAppropriateUserBundleFound() throws Exception {
        when(userBundle.getSymbolicName()).thenReturn("someDifferentSymbolicName");
        try {
            determinator.determine(relativePath);
            fail("Exception expected");
        } catch (final BundleDeterminationException e) {
            assertEquals(NoSuchElementException.class, e.getCause().getClass());
        }
    }

    @Test
    public void determineVersionNotParsable() throws Exception {
        when(relativePath.getName(1)).thenReturn(mock(Path.class));
        try {
            determinator.determine(relativePath);
            fail("Exception expected");
        } catch (final BundleDeterminationException e) {
            assertEquals(IllegalArgumentException.class, e.getCause().getClass());
        }
    }

    @Test
    public void determine() throws Exception {
        assertSame(userBundle, determinator.determine(relativePath));

        // Should have been called exactly once
        verify(context).getBundles();
    }

    @Test
    public void clearCacheWholeBundleDirectory() throws Exception {
        when(relativePath.getNameCount()).thenReturn(2);
        determinator.determine(relativePath);
        when(relativePath.getNameCount()).thenReturn(1);
        when(relativePath.startsWith(relativePath)).thenReturn(true);
        determinator.clearCacheFor(relativePath);
        when(relativePath.getNameCount()).thenReturn(2);
        determinator.determine(relativePath);
        verify(context, times(2)).getBundles();
    }

    @Test
    public void clearCacheVersionDirectoryOnly() throws Exception {
        when(relativePath.getNameCount()).thenReturn(2);
        determinator.determine(relativePath);
        determinator.clearCacheFor(relativePath);
        determinator.determine(relativePath);
        verify(context, times(2)).getBundles();
    }

    @Test
    public void clearCacheNothingToDo() {
        relativePath = mock(Path.class);
        when(relativePath.getName(0)).thenReturn(relativePath);

        // Should not cause an exception
        determinator.clearCacheFor(relativePath);
    }
}
