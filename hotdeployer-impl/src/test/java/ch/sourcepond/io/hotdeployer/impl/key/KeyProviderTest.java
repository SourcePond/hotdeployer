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
package ch.sourcepond.io.hotdeployer.impl.key;

import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.hotdeployer.impl.determinator.BundleDeterminationException;
import ch.sourcepond.io.hotdeployer.impl.determinator.BundleDeterminator;
import ch.sourcepond.io.hotdeployer.impl.determinator.BundleNotAvailableException;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

import java.nio.file.Path;

import static ch.sourcepond.io.hotdeployer.impl.determinator.BundleDeterminator.SPECIAL_BUNDLE_NAME_COUNT;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;
import static org.osgi.framework.VersionRange.valueOf;

/**
 *
 */
public class KeyProviderTest {
    private static final int RELATIVE_PATH_NAME_COUNT = 5;
    private final Path relativePath = mock(Path.class);
    private final Path adjustedRelativePath = mock(Path.class);
    private final Bundle bundle = mock(Bundle.class);
    private final DispatchKey fileKey = mock(DispatchKey.class);
    private final BundleDeterminator determinator = mock(BundleDeterminator.class);
    private final KeyProviderFactory factory = new KeyProviderFactory();
    private final KeyProvider provider = factory.createProvider(determinator);

    @Before
    public void setup() throws Exception {
        when(fileKey.getRelativePath()).thenReturn(relativePath);
        when(determinator.determine(relativePath)).thenReturn(bundle);
        when(bundle.getBundleId()).thenReturn(10l);
        when(relativePath.getNameCount()).thenReturn(RELATIVE_PATH_NAME_COUNT);
        when(relativePath.subpath(SPECIAL_BUNDLE_NAME_COUNT, RELATIVE_PATH_NAME_COUNT)).thenReturn(adjustedRelativePath);
    }

    @Test
    public void useAdjustedRelativePath() throws Exception {
        provider.before(fileKey);
        final DispatchKey key = provider.getKey(fileKey);
        assertNotNull(key);
        assertSame(adjustedRelativePath, key.getRelativePath());
        assertSame(bundle, key.getDirectoryKey());
        provider.afterDiscard(fileKey);
        assertNull(provider.getKey(fileKey));
    }

    @Test
    public void useOriginalRelativePath() throws Exception {
        when(bundle.getBundleId()).thenReturn(SYSTEM_BUNDLE_ID);
        provider.before(fileKey);
        final DispatchKey key = provider.getKey(fileKey);
        assertNotNull(key);
        assertSame(relativePath, key.getRelativePath());
        assertSame(bundle, key.getDirectoryKey());
        provider.afterDiscard(fileKey);
        assertNull(provider.getKey(fileKey));
    }

    @Test
    public void exceptionOccurred() throws Exception {
        final BundleDeterminationException expected = new BundleDeterminationException(new Exception());
        doThrow(expected).when(determinator).determine(any());
        provider.before(fileKey);
        try {
            provider.getKey(fileKey);
            fail("Exception expected!");
        } catch (final ResourceKeyException e) {
            assertSame(expected, e.getCause());
        }
    }

    @Test
    public void reThrowBundleNotAvailableException() throws Exception {
        final BundleNotAvailableException expected = new BundleNotAvailableException(relativePath, "any", valueOf("(1.0,2.0]"));
        doThrow(expected).when(determinator).determine(relativePath);
        provider.before(fileKey);
        try {
            provider.getKey(fileKey);
            fail("Exception expected");
        } catch (final BundleNotAvailableException e) {
            assertSame(expected, e);
        }
    }
}
