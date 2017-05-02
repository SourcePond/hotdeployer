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

import ch.sourcepond.io.hotdeployer.impl.Config;
import ch.sourcepond.io.hotdeployer.impl.determinator.PostponeQueue;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import java.nio.file.FileSystem;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static ch.sourcepond.io.hotdeployer.impl.observer.ObserverAdapterFactory.createDefaultExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class ObserverAdapterFactoryTest {
    private static final long BUNDLE_AVAILABILITY_TIMEOUT = 1L;
    private static final TimeUnit BUNDLE_AVAILABILITY_UNIT = SECONDS;
    private static final String BUNDLE_RESOURCE_DIRECTORY_PREFIX = "prefix";
    private final BundleContext context = mock(BundleContext.class);
    private final ExecutorService postponeExecutor = mock(ExecutorService.class);
    private final PostponeQueue queue = mock(PostponeQueue.class);
    private final HotdeployEventFactory eventProxyFactory = mock(HotdeployEventFactory.class);
    private final BundlePathDeterminator proxyFactory = mock(BundlePathDeterminator.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final Config config = mock(Config.class);
    private final ObserverAdapterFactory factory = new ObserverAdapterFactory(postponeExecutor, queue, eventProxyFactory, proxyFactory);

    @Before
    public void setup() {
        when(config.bundleAvailabilityTimeoutUnit()).thenReturn(BUNDLE_AVAILABILITY_UNIT);
        when(config.bundleAvailabilityTimeout()).thenReturn(BUNDLE_AVAILABILITY_TIMEOUT);
    }

    @Test
    public void verifyDefaultConstructor() {
        new ObserverAdapterFactory(context).shutdown();
    }

    @Test
    public void startQueue() {
        assertNotNull(queue);
        verify(postponeExecutor).execute(queue);
    }

    @Test
    public void setConfig() {
        when(config.bundleResourceDirectoryPrefix()).thenReturn(BUNDLE_RESOURCE_DIRECTORY_PREFIX);
        factory.setConfig(fs, config);
        verify(proxyFactory).setConfig(fs, BUNDLE_RESOURCE_DIRECTORY_PREFIX);
        verify(queue).setBundleAvailabilityTimeout(BUNDLE_AVAILABILITY_TIMEOUT, BUNDLE_AVAILABILITY_UNIT);
    }

    @Test(timeout = 3000)
    public void verifyDefaultExecutor() throws Exception {
        final ThreadPoolExecutor executor = createDefaultExecutor();
        final List<Runnable> verification = new CopyOnWriteArrayList<>();
        final Runnable[] runnables = new Runnable[6];
        for (int i = 0; i < runnables.length; i++) {
            runnables[i] = new Runnable() {
                @Override
                public void run() {
                    verification.add(this);
                }
            };
        }

        for (final Runnable runnable : runnables) {
            executor.execute(runnable);
        }

        executor.shutdown();
        executor.awaitTermination(2L, SECONDS);

        assertEquals(runnables.length, verification.size());
        for (int i = 0; i < runnables.length; i++) {
            assertSame(runnables[i], verification.get(i));
        }
    }

    @Test
    public void shutdown() {
        factory.shutdown();
        verify(postponeExecutor).shutdown();
    }
}
