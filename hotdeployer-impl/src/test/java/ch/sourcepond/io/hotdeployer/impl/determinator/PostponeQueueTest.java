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

import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.osgi.framework.*;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.*;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.osgi.framework.BundleEvent.RESOLVED;
import static org.osgi.framework.VersionRange.valueOf;

/**
 *
 */
public class PostponeQueueTest {
    private static final long TIMEOUT = 1;
    private static final TimeUnit UNIT = MILLISECONDS;
    private static final String ANY_SYMBOLIC_NAME = "anySymbolicName";
    private final ExecutorService executor = newCachedThreadPool();
    private final BundleContext context = mock(BundleContext.class);
    private final Path bundleKey = mock(Path.class);
    private final DispatchKey key = mock(DispatchKey.class);
    private final PathChangeEvent event = mock(PathChangeEvent.class);
    private final VersionRange range = valueOf("[1.0,2.0)");
    private final Bundle bundle = mock(Bundle.class);
    private final Logger logger = mock(Logger.class);
    private final BundleNotAvailableException exception = new BundleNotAvailableException(bundleKey, ANY_SYMBOLIC_NAME, range);
    private final DelayQueue delayQueue = new DelayQueue<>();
    private PostponeQueue queue = new PostponeQueue(delayQueue, logger);
    private BundleAvailableListener listener;
    private BundleAvailableListener listener1;

    @Before
    public void setup() {
        when(context.getBundles()).thenReturn(new Bundle[0]);
        when(bundle.getSymbolicName()).thenReturn(ANY_SYMBOLIC_NAME);
        when(bundle.getVersion()).thenReturn(Version.valueOf("1.0"));
        when(event.getKey()).thenReturn(key);
        queue.setBundleAvailabilityTimeout(TIMEOUT, UNIT);
    }

    @After
    public void tearDown() {
        executor.shutdown();
        interrupted();
    }

    @Test(timeout = 2000)
    public void verifyDefaultConstructor() {
        queue = new PostponeQueue(context);
        setup();
        postpone();
    }

    private BundleAvailableListener listener() {
        return argThat(inv -> event.equals(((BundleAvailableListener) inv).getEvent()));
    }

    @Test
    public void postpone() {
        queue.postpone(context, event, exception);
        final InOrder order = inOrder(context, event, context);
        order.verify(context).addBundleListener(listener());
        order.verify(context).getBundles();
        order.verifyNoMoreInteractions();
    }

    @Test
    public void postponeBundleAvailableDuringListenerRegistration() throws Exception {
        when(bundle.getSymbolicName()).thenReturn(ANY_SYMBOLIC_NAME);
        when(bundle.getVersion()).thenReturn(Version.valueOf("1.0"));
        when(context.getBundles()).thenReturn(new Bundle[]{ bundle });
        doAnswer(a -> {
            listener = a.getArgument(0);
            return null;
        }).when(context).addBundleListener(listener());
        queue.postpone(context, event, exception);
        assertNotNull(listener);
        listener.bundleChanged(new BundleEvent(RESOLVED, bundle));

        final InOrder order = inOrder(context, event, context);
        order.verify(context).addBundleListener(listener);
        order.verify(context).getBundles();
        order.verify(context).removeBundleListener(listener);
        order.verify(event).replay();
        order.verifyNoMoreInteractions();
    }

    @Test(timeout = 2000)
    public void postponeTimedOut() throws Exception {
        executor.execute(queue);
        queue.postpone(context, event, exception);
        sleep(100);
        verify(logger).warn("Postponed dispatch of {} failed because timeout! Reason of postpone was: ", event, exception);
    }

    @Test(timeout = 2000)
    public void takeInterrupted() throws Exception {
        final BlockingQueue<BundleAvailableListener> q = mock(BlockingQueue.class);
        queue = new PostponeQueue(q, logger);
        doThrow(InterruptedException.class).when(q).take();
        queue.run();
        assertTrue(currentThread().isInterrupted());
    }

    @Test
    public void dropEvents() {
        final DispatchKey key1 = mock(DispatchKey.class);
        final PathChangeEvent event1 = mock(PathChangeEvent.class);
        when(event1.getKey()).thenReturn(key1);
        final Path bundleKey1 = mock(Path.class);

        doAnswer(a -> {
            listener = a.getArgument(0);
            return null;
        }).when(context).addBundleListener(listener());

        doAnswer(a -> {
            listener1 = a.getArgument(0);
            return null;
        }).when(context).addBundleListener(argThat(inv -> event1.equals(((BundleAvailableListener) inv).getEvent())));

        final BundleNotAvailableException exception1 = new BundleNotAvailableException(bundleKey1, ANY_SYMBOLIC_NAME, range);
        queue.postpone(context, event, exception);
        queue.postpone(context, event1, exception1);
        assertNotNull(listener);
        assertNotNull(listener1);

        assertEquals(2, delayQueue.size());
        assertTrue(delayQueue.contains(listener));
        assertTrue(delayQueue.contains(listener1));

        queue.dropEvents(key1);
        assertEquals(1, delayQueue.size());
        assertTrue(delayQueue.contains(listener));
    }
}
