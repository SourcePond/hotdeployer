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

import ch.sourcepond.io.fileobserver.api.KeyDeliveryHook;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import ch.sourcepond.io.hotdeployer.api.FileChangeListener;
import ch.sourcepond.io.hotdeployer.impl.determinator.BundleDeterminator;
import ch.sourcepond.io.hotdeployer.impl.determinator.BundleDeterminatorFactory;
import ch.sourcepond.io.hotdeployer.impl.determinator.PostponeQueue;
import ch.sourcepond.io.hotdeployer.impl.determinator.PostponeQueueFactory;
import ch.sourcepond.io.hotdeployer.impl.key.KeyProvider;
import ch.sourcepond.io.hotdeployer.impl.key.KeyProviderFactory;
import ch.sourcepond.io.hotdeployer.impl.listener.ListenerAdapterFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import static org.mockito.Mockito.*;

/**
 *
 */
public class ActivatorTest {
    private static final String ANY_PREFIX = "__BUNDLE__";
    private static final String DIFFERENT_PREFIX = "__DIFFERENT__";
    private static final String BLACKLIST_PATTERNS = " AAA.zip ,  BBB.zip,CCC.zip ";
    private final FileSystem fs = mock(FileSystem.class);
    private final Path hotdeployDir = mock(Path.class);
    private final PostponeQueueFactory queueFactory = mock(PostponeQueueFactory.class);
    private final PostponeQueue queue = mock(PostponeQueue.class);
    private final ListenerAdapterFactory adapterFactory = mock(ListenerAdapterFactory.class);
    private final PathChangeListener adapter = mock(PathChangeListener.class);
    private final KeyProviderFactory keyProviderFactory = mock(KeyProviderFactory.class);
    private final KeyProvider keyProvider = mock(KeyProvider.class);
    private final BundleDeterminatorFactory bundleDeterminatorFactory = mock(BundleDeterminatorFactory.class);
    private final BundleDeterminator bundleDeterminator = mock(BundleDeterminator.class);
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final ServiceReference<FileChangeListener> observerRef = mock(ServiceReference.class);
    private final FileChangeListener observer = mock(FileChangeListener.class);
    private final WatchedDirectory watchedDirectory = mock(WatchedDirectory.class);
    private final ServiceRegistration<WatchedDirectory> watchedDirectoryRegistration = mock(ServiceRegistration.class);
    private final ServiceRegistration<KeyDeliveryHook> hookRegistration = mock(ServiceRegistration.class);
    private final ServiceRegistration<PathChangeListener> observerAdapterRegistration = mock(ServiceRegistration.class);
    private final BundleContext context = mock(BundleContext.class);
    private final Config config = mock(Config.class);
    private final Activator activator = new Activator(queueFactory, adapterFactory,
            bundleDeterminatorFactory, directoryFactory, keyProviderFactory);

    @Before
    public void setup() throws Exception {
        when(queueFactory.createQueue(context)).thenReturn(queue);
        when(hotdeployDir.getFileSystem()).thenReturn(fs);
        when(watchedDirectory.getDirectory()).thenReturn(hotdeployDir);
        when(adapterFactory.createAdapter(queue, keyProvider, observer)).thenReturn(adapter);
        when(keyProviderFactory.createProvider(bundleDeterminator)).thenReturn(keyProvider);
        when(bundleDeterminatorFactory.createDeterminator(context)).thenReturn(bundleDeterminator);
        when(directoryFactory.getHotdeployDir(config)).thenReturn(hotdeployDir);
        when(config.bundleResourceDirectoryPrefix()).thenReturn(ANY_PREFIX);
        when(config.blacklistPatterns()).thenReturn(BLACKLIST_PATTERNS);
        when(directoryFactory.newWatchedDirectory(config)).thenReturn(watchedDirectory);
        when(context.registerService(WatchedDirectory.class, watchedDirectory, null)).thenReturn(watchedDirectoryRegistration);
        when(context.registerService(KeyDeliveryHook.class, keyProvider, null)).thenReturn(hookRegistration);
        when(context.getService(observerRef)).thenReturn(observer);
        when(context.registerService(PathChangeListener.class, adapter, null)).thenReturn(observerAdapterRegistration);

        activator.activate(context, config);
        activator.addListener(observerRef);
    }

    @Test
    public void verifyDefaultConstructor() {
        new Activator();
    }

    @Test
    public void activate() {
        verify(queue).setConfig(config);
        verify(adapterFactory).setConfig(fs, config);
        verify(watchedDirectory).addBlacklistPattern("AAA.zip");
        verify(watchedDirectory).addBlacklistPattern("BBB.zip");
        verify(watchedDirectory).addBlacklistPattern("CCC.zip");
    }

    @Test
    public void deactivate() throws Exception {
        activator.deactivate();
        verify(watchedDirectoryRegistration).unregister();
        verify(hookRegistration).unregister();
        verify(observerAdapterRegistration).unregister();
        verify(queueFactory).shutdown();
    }

    @Test
    public void modify() throws Exception {
        final Config newConfig = mock(Config.class);
        when(newConfig.blacklistPatterns()).thenReturn(BLACKLIST_PATTERNS + "    ,    DDD.zip");
        when(newConfig.bundleResourceDirectoryPrefix()).thenReturn(DIFFERENT_PREFIX);
        when(directoryFactory.getHotdeployDir(newConfig)).thenReturn(hotdeployDir);
        verify(bundleDeterminator).setPrefix(ANY_PREFIX);
        activator.modify(newConfig);
        verify(queue).setConfig(newConfig);
        verify(watchedDirectory).relocate(hotdeployDir);
        verify(adapterFactory).setConfig(fs, newConfig);
        verify(watchedDirectory).removeBlacklistPattern("AAA.zip");
        verify(watchedDirectory).removeBlacklistPattern("BBB.zip");
        verify(watchedDirectory).removeBlacklistPattern("CCC.zip");
        verify(watchedDirectory, times(2)).addBlacklistPattern("AAA.zip");
        verify(watchedDirectory, times(2)).addBlacklistPattern("BBB.zip");
        verify(watchedDirectory, times(2)).addBlacklistPattern("CCC.zip");
        verify(watchedDirectory).addBlacklistPattern("DDD.zip");
        verify(observerAdapterRegistration).unregister();
    }

    @Test
    public void modifyPrefixChanged() throws Exception {
        final Config newConfig = mock(Config.class);
        when(newConfig.blacklistPatterns()).thenReturn(BLACKLIST_PATTERNS);
        when(newConfig.bundleResourceDirectoryPrefix()).thenReturn(DIFFERENT_PREFIX);
        when(directoryFactory.newWatchedDirectory(newConfig)).thenReturn(watchedDirectory);
        when(directoryFactory.getHotdeployDir(newConfig)).thenReturn(hotdeployDir);
        when(adapterFactory.createAdapter(queue, keyProvider, observer)).thenReturn(adapter);

        activator.modify(newConfig);
        final InOrder order = inOrder(bundleDeterminator, observerAdapterRegistration, watchedDirectory);
        order.verify(bundleDeterminator).setPrefix(DIFFERENT_PREFIX);
        order.verify(observerAdapterRegistration).unregister();
        order.verify(watchedDirectory).relocate(hotdeployDir);
    }

    @Test
    public void addRemoveObserver() {
        activator.addListener(observerRef);
        activator.removeObserver(observerRef);
        verify(observerAdapterRegistration).unregister();
    }

    @Test
    public void removeObserverNothingRegistered() {
        // This should not cause an exception
        activator.removeObserver(mock(ServiceReference.class));
    }
}
