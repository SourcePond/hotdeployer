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

import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.osgi.framework.*;

import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.osgi.framework.BundleEvent.RESOLVED;
import static org.osgi.framework.BundleEvent.STARTED;
import static org.osgi.framework.Version.valueOf;

/**
 *
 */
public class BundleAvailableListenerTest {
    private static final String ANY_SYMBOLIC_NAME = "anySymbolicName";
    private static final Version ANY_VERSION = valueOf("1.0");
    private static final long TIMEOUT = 2000L;
    private final VersionRange range = VersionRange.valueOf("[1.0,2.0)");
    private final Path bundlePath = mock(Path.class);
    private final Bundle bundle = mock(Bundle.class);
    private final BlockingQueue<BundleAvailableListener> queue = new DelayQueue<>();
    private final BundleContext context = mock(BundleContext.class);
    private final PathChangeEvent event = mock(PathChangeEvent.class);
    private final BundleNotAvailableException exception = new BundleNotAvailableException(bundlePath, ANY_SYMBOLIC_NAME, range);
    private final BundleAvailableListener listener = new BundleAvailableListener(queue, context, event, exception, nowPlusTimeout());

    private static long nowPlusTimeout() {
        return Instant.now().toEpochMilli() +  TIMEOUT;
    }

    @Before
    public void setup() {
        when(bundle.getSymbolicName()).thenReturn(ANY_SYMBOLIC_NAME);
        when(bundle.getVersion()).thenReturn(ANY_VERSION);
        queue.offer(listener);
    }

    @Test
    public void getEvent() {
        assertSame(event, listener.getEvent());
    }

    @Test
    public void getException() {
        assertSame(exception, listener.getException());
    }

    private void verifyReplay() {
        final InOrder order = inOrder(context, event);
        order.verify(context).removeBundleListener(listener);
        order.verify(event).replay();
        assertTrue(queue.isEmpty());
    }

    @Test
    public void tryReplay() {
        assertTrue(listener.tryReplay(bundle));
        verifyReplay();
    }

    private void verifyNoAction() {
        assertFalse(listener.tryReplay(bundle));
        verifyZeroInteractions(context, event);
        assertFalse(queue.isEmpty());
    }

    @Test
    public void tryReplaySymbolicNameDoesNotMatch() {
        when(bundle.getSymbolicName()).thenReturn("somethingDifferent");
        verifyNoAction();
    }

    @Test
    public void tryReplayVersionDoesNotMatch() {
        when(bundle.getVersion()).thenReturn(Version.valueOf("4.0.0"));
        verifyNoAction();
    }

    @Test
    public void tryReplayEventAlreadyProcessed() {
        tryReplay();
        assertFalse(listener.tryReplay(bundle));
        verifyNoMoreInteractions(context, event);
    }

    @Test
    public void bundleChanged() {
        final BundleEvent event = new BundleEvent(RESOLVED, bundle);
        listener.bundleChanged(event);
        verifyReplay();
    }

    @Test
    public void bundleChangedEventTypeIgnored() {
        final BundleEvent be = new BundleEvent(STARTED, bundle);
        listener.bundleChanged(be);
        verifyZeroInteractions(event, context);
    }

    @Test(timeout = 2000)
    public void getDelay() throws Exception {
        assertTrue(1000 >= listener.getDelay(SECONDS));
        final long sleepTime = 50;
        int runs = 0;
        while(listener.getDelay(SECONDS) > 0) {
            sleep(sleepTime);
            assertTrue(100 > listener.getDelay(SECONDS));
            runs++;
        }
        assertTrue(runs > 0);
    }

    @Test
    public void compareTo() throws Exception {
        assertEquals(0, listener.compareTo(listener));

        final BundleAvailableListener l1 = new BundleAvailableListener(queue, context, event, exception, nowPlusTimeout());
        sleep(1000);
        final BundleAvailableListener l2 = new BundleAvailableListener(queue, context, event, exception, nowPlusTimeout());
        assertEquals(1, l2.compareTo(l1));
        assertEquals(-1, l1.compareTo(l2));
    }
}
