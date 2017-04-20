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

import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.api.KeyDeliveryHook;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import ch.sourcepond.io.hotdeployer.api.FileChangeObserver;
import ch.sourcepond.io.hotdeployer.impl.determinator.BundleDeterminator;
import ch.sourcepond.io.hotdeployer.impl.determinator.BundleDeterminatorFactory;
import ch.sourcepond.io.hotdeployer.impl.key.KeyProvider;
import ch.sourcepond.io.hotdeployer.impl.key.KeyProviderFactory;
import ch.sourcepond.io.hotdeployer.impl.observer.ObserverAdapterFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.nio.file.Path;

import static org.mockito.Mockito.*;

/**
 *
 */
public class ActivatorTest {
    private static final String ANY_PREFIX = "$BUNDLE$_";
    private static final String DIFFERENT_PREFIX = "$DIFFERENT$_";
    private final Path hotdeployDir = mock(Path.class);
    private final ObserverAdapterFactory adapterFactory = mock(ObserverAdapterFactory.class);
    private final FileObserver adapter = mock(FileObserver.class);
    private final KeyProviderFactory keyProviderFactory = mock(KeyProviderFactory.class);
    private final KeyProvider keyProvider = mock(KeyProvider.class);
    private final BundleDeterminatorFactory bundleDeterminatorFactory = mock(BundleDeterminatorFactory.class);
    private final BundleDeterminator bundleDeterminator = mock(BundleDeterminator.class);
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final FileChangeObserver observer = mock(FileChangeObserver.class);
    private final WatchedDirectory watchedDirectory = mock(WatchedDirectory.class);
    private final ServiceRegistration<WatchedDirectory> watchedDirectoryRegistration = mock(ServiceRegistration.class);
    private final ServiceRegistration<KeyDeliveryHook> hookRegistration = mock(ServiceRegistration.class);
    private final ServiceRegistration<FileObserver> observerAdapterRegistration = mock(ServiceRegistration.class);
    private final BundleContext context = mock(BundleContext.class);
    private final Config config = mock(Config.class);
    private final Activator activator = new Activator(adapterFactory, bundleDeterminatorFactory, directoryFactory, keyProviderFactory);

    @Before
    public void setup() throws Exception {
        when(adapterFactory.createAdapter(ANY_PREFIX, keyProvider, observer)).thenReturn(adapter);
        when(keyProviderFactory.createProvider(bundleDeterminator)).thenReturn(keyProvider);
        when(bundleDeterminatorFactory.createDeterminator(context)).thenReturn(bundleDeterminator);
        when(directoryFactory.getHotdeployDir(config)).thenReturn(hotdeployDir);
        when(config.bundleResourceDirectoryPrefix()).thenReturn(ANY_PREFIX);
        when(directoryFactory.newWatchedDirectory(config)).thenReturn(watchedDirectory);
        when(context.registerService(WatchedDirectory.class, watchedDirectory, null)).thenReturn(watchedDirectoryRegistration);
        when(context.registerService(FileObserver.class, adapter, null)).thenReturn(observerAdapterRegistration);
        when(context.registerService(KeyDeliveryHook.class, keyProvider, null)).thenReturn(hookRegistration);

        activator.activate(context, config);
        activator.addObserver(observer);
    }

    @Test
    public void verifyDefaultConstructor() {
        new Activator();
    }

    @Test
    public void activate() throws Exception {
        activator.deactivate();
        verify(watchedDirectoryRegistration).unregister();
        verify(hookRegistration).unregister();
        verify(observerAdapterRegistration).unregister();
    }

    @Test
    public void modify() throws Exception {
        verify(bundleDeterminator).setPrefix(ANY_PREFIX);
        activator.modify(config);
        verify(watchedDirectory).relocate(hotdeployDir);
        verifyZeroInteractions(observerAdapterRegistration);
        verifyNoMoreInteractions(bundleDeterminator, watchedDirectory);
    }

    @Test
    public void modifyPrefixChanged() throws Exception {
        final Config newConfig = mock(Config.class);
        when(newConfig.bundleResourceDirectoryPrefix()).thenReturn(DIFFERENT_PREFIX);
        when(directoryFactory.newWatchedDirectory(newConfig)).thenReturn(watchedDirectory);
        when(directoryFactory.getHotdeployDir(newConfig)).thenReturn(hotdeployDir);
        when(adapterFactory.createAdapter(DIFFERENT_PREFIX, keyProvider, observer)).thenReturn(adapter);

        activator.modify(newConfig);
        final InOrder order = inOrder(bundleDeterminator, observerAdapterRegistration, watchedDirectory);
        order.verify(bundleDeterminator).setPrefix(DIFFERENT_PREFIX);
        order.verify(observerAdapterRegistration).unregister();
        order.verify(watchedDirectory).relocate(hotdeployDir);
    }

    @Test
    public void addRemoveObserver() {
        activator.addObserver(observer);
        activator.removeObserver(observer);
        verify(observerAdapterRegistration).unregister();
    }

    @Test
    public void removeObserverNothingRegistered() {
        // This should not cause an exception
        activator.removeObserver(mock(FileChangeObserver.class));
    }
}
