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
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.nio.file.Path;

import static org.mockito.Mockito.*;

/**
 *
 */
public class ActivatorTest {
    private static final String ANY_PREFIX = "$BUNDLE$_";
    private final Path hotdeployDir = mock(Path.class);
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final FileChangeObserver observer = mock(FileChangeObserver.class);
    private final WatchedDirectory watchedDirectory = mock(WatchedDirectory.class);
    private final ServiceRegistration<WatchedDirectory> watchedDirectoryRegistration = mock(ServiceRegistration.class);
    private final ServiceRegistration<KeyDeliveryHook> hookRegistration = mock(ServiceRegistration.class);
    private final ServiceRegistration<FileObserver> observerAdapterRegistration = mock(ServiceRegistration.class);
    private final BundleContext context = mock(BundleContext.class);
    private final Config config = mock(Config.class);
    private final Activator activator = new Activator(directoryFactory);

    @Before
    public void setup() throws Exception {
        when(directoryFactory.getHotdeployDir(config)).thenReturn(hotdeployDir);
        when(config.bundleResourceDirectoryPrefix()).thenReturn(ANY_PREFIX);
        when(directoryFactory.newWatchedDirectory(config)).thenReturn(watchedDirectory);
        when(context.registerService(WatchedDirectory.class, watchedDirectory, null)).thenReturn(watchedDirectoryRegistration);
        when(context.registerService(same(FileObserver.class), argThat((FileObserver t) -> t instanceof ObserverAdapter), isNull())).thenReturn(observerAdapterRegistration);
        when(context.registerService(same(KeyDeliveryHook.class), argThat((KeyDeliveryHook h) -> h instanceof KeyProvider), isNull())).thenReturn(hookRegistration);

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
        activator.modify(config);
        verify(watchedDirectory).relocate(hotdeployDir);
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
