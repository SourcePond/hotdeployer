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

import ch.sourcepond.commons.smartswitch.api.SmartSwitchBuilder;
import ch.sourcepond.commons.smartswitch.api.SmartSwitchBuilderFactory;
import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import ch.sourcepond.io.hotdeployer.api.HotdeployObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static java.lang.Thread.setDefaultUncaughtExceptionHandler;
import static java.lang.Thread.sleep;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

/**
 *
 */
public class HotdeployerTest implements Thread.UncaughtExceptionHandler {
    private final SmartSwitchBuilderFactory ssbBuilderFactory = mock(SmartSwitchBuilderFactory.class);
    private final Config config = mock(Config.class);
    private final DirectoryFactory factory = mock(DirectoryFactory.class);
    private final WatchedDirectory watchedDirectory = mock(WatchedDirectory.class);
    private final Path directory = mock(Path.class);
    private final Path relativeFile = mock(Path.class);
    private final Path file = mock(Path.class);
    private final FileKey fileKey = mock(FileKey.class);
    private final BundleContext context = mock(BundleContext.class);
    private final ServiceRegistration<WatchedDirectory> registration = mock(ServiceRegistration.class);
    private final HotdeployObserver observer = mock(HotdeployObserver.class);
    private final Hotdeployer deployer = new Hotdeployer(factory);
    private ExecutorService observerExecutor;
    private volatile Throwable threadKiller;

    @Before
    public void setup() throws Exception {
        when(fileKey.relativePath()).thenReturn(relativeFile);

        when(factory.newWatchedDirectory(config)).thenReturn(watchedDirectory);
        when(factory.getHotdeployDir(config)).thenReturn(directory);
        when(context.registerService(WatchedDirectory.class, watchedDirectory, null)).thenReturn(registration);

        final SmartSwitchBuilder<ExecutorService> builder = mock(SmartSwitchBuilder.class);
        when(builder.setFilter("(sourcepond.io.hotdeployer.observerexecutor=*)")).thenReturn(builder);
        when(builder.setShutdownHook(notNull())).thenReturn(builder);
        when(builder.build(notNull())).thenAnswer(inv -> {
            Supplier<ExecutorService> supplier = inv.getArgument(0);
            observerExecutor = supplier.get();
            assertNotNull(observerExecutor);
            return observerExecutor;
        });
        when(ssbBuilderFactory.newBuilder(ExecutorService.class)).thenReturn(builder);
        deployer.addObserver(observer);
        deployer.initExecutor(ssbBuilderFactory);

        setDefaultUncaughtExceptionHandler(this);
    }

    @After
    public void tearDown() {
        if (observerExecutor != null) {
            observerExecutor.shutdown();
        }
    }

    @Test
    public void verifyDefaultConstructor() {
        // Should not cause an exception
        new Hotdeployer();
    }

    @Test
    public void activateModifyDeactivate() throws Exception {
        deployer.activate(context, config);
        deployer.modify(config);
        deployer.deactivate();

        final InOrder order = inOrder(watchedDirectory, registration);
        order.verify(watchedDirectory).relocate(directory);
        order.verify(registration).unregister();
    }

    @Test
    public void supplement() {
        final FileKey knownKey = mock(FileKey.class);
        final FileKey supplementKey = mock(FileKey.class);
        deployer.supplement(knownKey, supplementKey);
        verifyZeroInteractions(knownKey, supplementKey);
    }

    @Test
    public void modified() throws IOException {
        deployer.modified(fileKey, file);
        verify(observer, timeout(1000)).modified(relativeFile, file);
    }

    @Test
    public void modifiedIOExceptionOccurred() throws IOException {
        doThrow(IOException.class).when(observer).modified(any(), any());
        deployer.modified(fileKey, file);
        verify(observer, timeout(1000)).modified(relativeFile, file);
        assertNull(threadKiller);
    }

    @Test
    public void discard() {
        deployer.discard(fileKey);
        verify(observer, timeout(1000)).discard(relativeFile);
    }

    @Test
    public void discardExceptionOccurred() {
        doThrow(RuntimeException.class).when(observer).discard(any());
        deployer.discard(fileKey);
        verify(observer, timeout(1000)).discard(relativeFile);
        assertNull(threadKiller);
    }

    @Test
    public void removeObserver() throws Exception {
        deployer.removeObserver(observer);
        deployer.modified(fileKey, file);
        sleep(1000);
        verifyZeroInteractions(observer);
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        threadKiller = e;
    }
}
