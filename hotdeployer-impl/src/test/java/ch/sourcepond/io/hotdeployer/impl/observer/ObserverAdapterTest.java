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

import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.DispatchRestriction;
import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import ch.sourcepond.io.hotdeployer.api.FileChangeListener;
import ch.sourcepond.io.hotdeployer.impl.key.KeyProvider;
import ch.sourcepond.io.hotdeployer.impl.key.ResourceKeyException;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import static ch.sourcepond.io.hotdeployer.impl.DirectoryFactory.DIRECTORY_KEY;
import static org.mockito.Mockito.*;

/**
 *
 */
public class ObserverAdapterTest {
    private static final String ANY_PREFIX = "anyPrefix";
    private final DispatchKey fileKey = mock(DispatchKey.class);
    private final PathChangeEvent event = mock(PathChangeEvent.class);
    private final DispatchKey resourceKey = mock(DispatchKey.class);
    private final KeyProvider provider = mock(KeyProvider.class);
    private final FileChangeListener fileChangeListener = mock(FileChangeListener.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final Path file = mock(Path.class);
    private final BundlePathDeterminator proxyFactory = mock(BundlePathDeterminator.class);
    private final DispatchRestrictionProxy proxy = mock(DispatchRestrictionProxy.class);
    private final DispatchRestriction restriction = mock(DispatchRestriction.class);
    private final ObserverAdapterFactory factory = new ObserverAdapterFactory(proxyFactory);
    private final PathChangeListener adapter = factory.createAdapter(provider, fileChangeListener);

    @Before
    public void setup() throws Exception {
        when(provider.getKey(fileKey)).thenReturn(resourceKey);
        when(proxyFactory.createProxy( restriction)).thenReturn(proxy);
    }

    @Test
    public void restrict() {
        adapter.restrict(restriction, fs);
        verify(restriction).accept(DIRECTORY_KEY);
        verify(fileChangeListener).setup(proxy);
    }

    @Test
    public void supplement() {
        adapter.supplement(null, null);
        verifyZeroInteractions(fileKey, resourceKey, provider, fileChangeListener);
    }

    @Test
    public void modified() throws Exception {
        adapter.modified(event);
        verify(fileChangeListener).modified(event);
    }

    @Test
    public void modifiedExceptionOccurred() throws Exception {
        final ResourceKeyException expected = new ResourceKeyException("any", new Exception());
        doThrow(expected).when(provider).getKey(fileKey);

        // Should not cause an exception to be thrown
        adapter.modified(event);
    }

    @Test
    public void discard() {
        adapter.discard(fileKey);
        verify(fileChangeListener).discard(resourceKey);
    }


    @Test
    public void discardExceptionOccurred() throws Exception {
        final ResourceKeyException expected = new ResourceKeyException("any", new Exception());
        doThrow(expected).when(provider).getKey(fileKey);

        // Should not cause an exception to be thrown
        adapter.discard(fileKey);
    }
}
