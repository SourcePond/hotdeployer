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
import ch.sourcepond.io.hotdeployer.impl.determinator.BundleNotAvailableException;
import ch.sourcepond.io.hotdeployer.impl.determinator.PostponeQueue;
import ch.sourcepond.io.hotdeployer.impl.determinator.PostponeQueueFactory;
import ch.sourcepond.io.hotdeployer.impl.key.KeyProvider;
import ch.sourcepond.io.hotdeployer.impl.key.ResourceKeyException;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.VersionRange;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import static ch.sourcepond.io.hotdeployer.impl.DirectoryFactory.DIRECTORY_KEY;
import static org.mockito.Mockito.*;

/**
 *
 */
public class ObserverAdapterTest {
    private final DispatchKey fileKey = mock(DispatchKey.class);
    private final PathChangeEvent event = mock(PathChangeEvent.class);
    private final HotdeployEvent eventProxy = mock(HotdeployEvent.class);
    private final DispatchKey resourceKey = mock(DispatchKey.class);
    private final KeyProvider provider = mock(KeyProvider.class);
    private final FileChangeListener fileChangeListener = mock(FileChangeListener.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final HotdeployEventFactory eventProxyFactory = mock(HotdeployEventFactory.class);
    private final BundlePathDeterminator proxyFactory = mock(BundlePathDeterminator.class);
    private final DispatchRestrictionProxy proxy = mock(DispatchRestrictionProxy.class);
    private final DispatchRestriction restriction = mock(DispatchRestriction.class);
    private final PostponeQueueFactory queueFactory = mock(PostponeQueueFactory.class);
    private final PostponeQueue queue = mock(PostponeQueue.class);
    private final BundleContext context = mock(BundleContext.class);
    private final BundleNotAvailableException bundleNotAvailableException = new BundleNotAvailableException(mock(Path.class), "any", VersionRange.valueOf("(1.0,2.0]"));
    private final ObserverAdapterFactory factory = new ObserverAdapterFactory(queueFactory, eventProxyFactory, proxyFactory);
    private PathChangeListener adapter;

    @Before
    public void setup() throws Exception {
        when(event.getKey()).thenReturn(fileKey);
        when(eventProxyFactory.create(event, resourceKey)).thenReturn(eventProxy);
        when(provider.getKey(fileKey)).thenReturn(resourceKey);
        when(proxyFactory.createProxy(restriction)).thenReturn(proxy);
        when(eventProxyFactory.create(event, fileKey)).thenReturn(eventProxy);
        when(queueFactory.createQueue(context)).thenReturn(queue);
        adapter = factory.createAdapter(context, provider, fileChangeListener);
    }

    @Test
    public void restrict() {
        adapter.restrict(restriction, fs);
        verify(restriction).accept(DIRECTORY_KEY);
        verify(fileChangeListener).restrict(proxy, fs);
    }

    @Test
    public void supplement() {
        adapter.supplement(null, null);
        verifyZeroInteractions(fileKey, resourceKey, provider, fileChangeListener);
    }

    @Test
    public void modified() throws Exception {
        adapter.modified(event);
        verify(fileChangeListener).modified(eventProxy);
    }

    @Test
    public void modifiedBundleIsNotAvailable() throws Exception {
        doThrow(bundleNotAvailableException).when(provider).getKey(fileKey);
        adapter.modified(event);
        verify(queue).postpone(event, bundleNotAvailableException);
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

    @Test
    public void discardWhenBundleIsNotAvailable() throws Exception {
        doThrow(bundleNotAvailableException).when(provider).getKey(fileKey);
        adapter.discard(fileKey);
        verify(queue).dropEvents(fileKey);
    }
}
