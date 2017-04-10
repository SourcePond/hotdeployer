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
import ch.sourcepond.io.hotdeployer.api.HotdeployObserver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
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
    private BundleDeterminator bundleDeterminator;
    private KeyProvider keyProvider;
    private WatchedDirectory watchedDirectory;
    private ServiceRegistration<KeyDeliveryHook> hookRegistration;
    private ServiceRegistration<WatchedDirectory> watchedDirectoryRegistration;
    private ConcurrentMap<HotdeployObserver, ServiceRegistration<FileObserver>> observers = new ConcurrentHashMap<>();
    private BundleContext context;

    // Constructor for OSGi DS
    public Activator() {
        this(new DirectoryFactory());
    }

    // Constructor for testing
    Activator(final DirectoryFactory pDirectoryFactory) {
        directoryFactory = pDirectoryFactory;
    }

    @Activate
    public void activate(final BundleContext pContext, final Config pConfig) throws IOException, URISyntaxException {
        context = pContext;
        watchedDirectory = directoryFactory.newWatchedDirectory(pConfig);
        watchedDirectoryRegistration = pContext.registerService(WatchedDirectory.class, watchedDirectory, null);
        bundleDeterminator = new BundleDeterminator(pContext, pConfig.bundleResourceDirectoryPrefix());
        keyProvider = new KeyProvider(bundleDeterminator);

        pContext.addBundleListener(bundleDeterminator);

        hookRegistration = pContext.registerService(KeyDeliveryHook.class, keyProvider, null);
        LOG.info("Activator started");
    }

    @Modified
    public void modify(final Config pConfig) throws IOException, URISyntaxException {
        watchedDirectory.relocate(directoryFactory.getHotdeployDir(pConfig));
    }

    @Deactivate
    public void deactivate() {
        watchedDirectoryRegistration.unregister();
        hookRegistration.unregister();
        observers.forEach((k, v) -> v.unregister());
        observers.clear();
        context.removeBundleListener(bundleDeterminator);
        LOG.info("Activator shutdown");
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE)
    public void addObserver(final HotdeployObserver pObserver) {
        observers.put(pObserver, context.registerService(
                FileObserver.class,
                new ObserverAdapter(keyProvider, pObserver),
                null));
    }

    public void removeObserver(final HotdeployObserver pObserver) {
        final ServiceRegistration<FileObserver> adapterRegistration = observers.remove(pObserver);
        if (adapterRegistration == null) {
            LOG.warn("No adapter was registered for hotdeployer-observer {}", pObserver);
        } else {
            adapterRegistration.unregister();
        }
    }
}