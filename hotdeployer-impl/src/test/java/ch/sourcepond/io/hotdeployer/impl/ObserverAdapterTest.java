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

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.hotdeployer.api.FileChangeObserver;
import ch.sourcepond.io.hotdeployer.impl.key.KeyProvider;
import ch.sourcepond.io.hotdeployer.impl.key.ResourceKeyException;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

import java.nio.file.Path;

import static org.mockito.Mockito.*;

/**
 *
 */
public class ObserverAdapterTest {
    private static final String ANY_PREFIX = "anyPrefix";
    private final FileKey<Object> fileKey = mock(FileKey.class);
    private final FileKey<Bundle> resourceKey = mock(FileKey.class);
    private final KeyProvider provider = mock(KeyProvider.class);
    private final FileChangeObserver fileChangeObserver = mock(FileChangeObserver.class);
    private final Path file = mock(Path.class);
    private final ObserverAdapter adapter = new ObserverAdapter(ANY_PREFIX, provider, fileChangeObserver);

    @Before
    public void setup() throws Exception {
        when(provider.getKey(fileKey)).thenReturn(resourceKey);
    }

    @Test
    public void supplement() {
        adapter.supplement(null, null);
        verifyZeroInteractions(fileKey, resourceKey, provider, fileChangeObserver);
    }

    @Test
    public void modified() throws Exception {
        adapter.modified(fileKey, file);
        verify(fileChangeObserver).modified(resourceKey, file);
    }

    @Test
    public void modifiedExceptionOccurred() throws Exception {
        final ResourceKeyException expected = new ResourceKeyException("any", new Exception());
        doThrow(expected).when(provider).getKey(fileKey);

        // Should not cause an exception to be thrown
        adapter.modified(fileKey, file);
    }

    @Test
    public void discard() {
        adapter.discard(fileKey);
        verify(fileChangeObserver).discard(resourceKey);
    }


    @Test
    public void discardExceptionOccurred() throws Exception {
        final ResourceKeyException expected = new ResourceKeyException("any", new Exception());
        doThrow(expected).when(provider).getKey(fileKey);

        // Should not cause an exception to be thrown
        adapter.discard(fileKey);
    }
}
