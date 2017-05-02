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
import ch.sourcepond.io.hotdeployer.impl.observer.ObserverAdapterFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
@Component
@Designate(ocd = Config.class)
public class Activator {
    private static final Logger LOG = getLogger(Activator.class);
    private final DirectoryFactory directoryFactory;
    private final BundleDeterminatorFactory bundleDeterminatorFactory;
    private final KeyProviderFactory keyProviderFactory;
    private final PostponeQueueFactory queueFactory;
    private final ObserverAdapterFactory adapterFactory;
    private final ConcurrentMap<ServiceReference<FileChangeListener>, ServiceRegistration<PathChangeListener>> observers = new ConcurrentHashMap<>();
    private volatile Config config;
    private volatile BundleContext context;
    private volatile PostponeQueue queue;
    private volatile BundleDeterminator bundleDeterminator;
    private volatile KeyProvider keyProvider;
    private volatile WatchedDirectory watchedDirectory;
    private volatile ServiceRegistration<KeyDeliveryHook> hookRegistration;
    private volatile ServiceRegistration<WatchedDirectory> watchedDirectoryRegistration;

    // Constructor for OSGi DS
    public Activator() {
        this(new PostponeQueueFactory(),
                new ObserverAdapterFactory(),
                new BundleDeterminatorFactory(),
                new DirectoryFactory(),
                new KeyProviderFactory());
    }

    // Constructor for testing
    Activator(final PostponeQueueFactory pPostQueueFactory,
              final ObserverAdapterFactory pAdapterFactory,
              final BundleDeterminatorFactory pBundleDeterminatorFactory,
              final DirectoryFactory pDirectoryFactory,
              final KeyProviderFactory pKeyProviderFactory) {
        queueFactory = pPostQueueFactory;
        adapterFactory = pAdapterFactory;
        bundleDeterminatorFactory = pBundleDeterminatorFactory;
        directoryFactory = pDirectoryFactory;
        keyProviderFactory = pKeyProviderFactory;
    }

    @Activate
    public void activate(final BundleContext pContext, final Config pConfig) throws IOException, URISyntaxException {
        context = pContext;
        config = pConfig;
        queue = queueFactory.createQueue(pContext);
        queue.setConfig(pConfig);

        watchedDirectory = directoryFactory.newWatchedDirectory(pConfig);
        adapterFactory.setConfig(watchedDirectory.getDirectory().getFileSystem(), pConfig);
        watchedDirectoryRegistration = pContext.registerService(WatchedDirectory.class, watchedDirectory, null);
        bundleDeterminator = bundleDeterminatorFactory.createDeterminator(pContext);
        bundleDeterminator.setPrefix(pConfig.bundleResourceDirectoryPrefix());
        keyProvider = keyProviderFactory.createProvider(bundleDeterminator);
        hookRegistration = pContext.registerService(KeyDeliveryHook.class, keyProvider, null);
        LOG.info("Activator started");
    }

    @Modified
    public void modify(final Config pConfig) throws IOException, URISyntaxException {
        final String previousPrefix = config.bundleResourceDirectoryPrefix();
        final String newPrefix = pConfig.bundleResourceDirectoryPrefix();

        config = pConfig;
        queue.setConfig(pConfig);

        final Collection<ServiceReference<FileChangeListener>> toBeRegistered = previousPrefix.equals(newPrefix) ? null :
                new ArrayList<>(observers.keySet());

        if (toBeRegistered != null) {
            bundleDeterminator.setPrefix(newPrefix);
            unregisterAllObservers();
        }

        final Path hotdeployDir = directoryFactory.getHotdeployDir(pConfig);
        try {
            watchedDirectory.relocate(hotdeployDir);
        } finally {
            adapterFactory.setConfig(hotdeployDir.getFileSystem(), pConfig);
            if (toBeRegistered != null) {
                toBeRegistered.forEach(this::addListener);
            }
        }
    }

    @Deactivate
    public void deactivate() {
        watchedDirectoryRegistration.unregister();
        hookRegistration.unregister();
        observers.forEach((k, v) -> v.unregister());
        observers.clear();
        queueFactory.shutdown();
        LOG.info("Activator shutdown");
    }

    private void unregisterAllObservers() {
        observers.forEach((k, v) -> v.unregister());
        observers.clear();
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE, unbind = "removeObserver")
    public void addListener(final ServiceReference<FileChangeListener> pReference) {
        final FileChangeListener listener = context.getService(pReference);
        observers.put(pReference, context.registerService(
                PathChangeListener.class,
                adapterFactory.createAdapter(queue, keyProvider, listener),
                null));
    }

    public void removeObserver(final ServiceReference<FileChangeListener> pObserver) {
        final ServiceRegistration<PathChangeListener> adapterRegistration = observers.remove(pObserver);
        if (adapterRegistration == null) {
            LOG.warn("No adapter was registered for hotdeployer-observer {}", pObserver);
        } else {
            adapterRegistration.unregister();
        }
    }
}
