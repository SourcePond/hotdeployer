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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
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
    private volatile Config config;
    private BundleDeterminator bundleDeterminator;
    private KeyProvider keyProvider;
    private WatchedDirectory watchedDirectory;
    private ServiceRegistration<KeyDeliveryHook> hookRegistration;
    private ServiceRegistration<WatchedDirectory> watchedDirectoryRegistration;
    private ConcurrentMap<FileChangeObserver, ServiceRegistration<FileObserver>> observers = new ConcurrentHashMap<>();
    private BundleContext context;

    // Constructor for OSGi DS
    public Activator() {
        this(new BundleDeterminatorFactory(),
                new DirectoryFactory(),
                new KeyProviderFactory());
    }

    // Constructor for testing
    Activator(final BundleDeterminatorFactory pBundleDeterminatorFactory,
              final DirectoryFactory pDirectoryFactory,
              final KeyProviderFactory pKeyProviderFactory) {
        bundleDeterminatorFactory = pBundleDeterminatorFactory;
        directoryFactory = pDirectoryFactory;
        keyProviderFactory = pKeyProviderFactory;
    }

    @Activate
    public void activate(final BundleContext pContext, final Config pConfig) throws IOException, URISyntaxException {
        context = pContext;
        config = pConfig;
        watchedDirectory = directoryFactory.newWatchedDirectory(pConfig);
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
        final Collection<FileChangeObserver> toBeRegistered = previousPrefix.equals(newPrefix) ? null :
                new ArrayList<>(observers.keySet());

        if (toBeRegistered != null) {
            bundleDeterminator.setPrefix(newPrefix);
            unregisterAllObservers();
        }

        try {
            watchedDirectory.relocate(directoryFactory.getHotdeployDir(pConfig));
        } finally {
            if (toBeRegistered != null) {
                toBeRegistered.forEach(this::addObserver);
            }
        }
    }

    @Deactivate
    public void deactivate() {
        watchedDirectoryRegistration.unregister();
        hookRegistration.unregister();
        observers.forEach((k, v) -> v.unregister());
        observers.clear();
        LOG.info("Activator shutdown");
    }

    private void unregisterAllObservers() {
        observers.forEach((k, v) -> v.unregister());
        observers.clear();
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE)
    public void addObserver(final FileChangeObserver pObserver) {
        observers.put(pObserver, context.registerService(
                FileObserver.class,
                new ObserverAdapter(config.bundleResourceDirectoryPrefix(), keyProvider, pObserver),
                null));
    }

    public void removeObserver(final FileChangeObserver pObserver) {
        final ServiceRegistration<FileObserver> adapterRegistration = observers.remove(pObserver);
        if (adapterRegistration == null) {
            LOG.warn("No adapter was registered for hotdeployer-observer {}", pObserver);
        } else {
            adapterRegistration.unregister();
        }
    }
}
