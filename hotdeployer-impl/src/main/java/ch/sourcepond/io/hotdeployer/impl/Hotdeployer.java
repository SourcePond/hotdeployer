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
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
@Component
@Designate(ocd = Config.class)
public class Hotdeployer {
    private static final Logger LOG = getLogger(Hotdeployer.class);
    private final DirectoryFactory directoryFactory;
    private BundleDeterminator bundleDeterminator;
    private ResourceKeyFactory resourceKeyFactory;
    private WatchedDirectory delegate;
    private ServiceRegistration<KeyDeliveryHook> hookRegistration;
    private ServiceRegistration<WatchedDirectory> watchedDirectoryRegistration;
    private Collection<ServiceRegistration<FileObserver>> adapterReferences = newKeySet();
    private ConcurrentMap<HotdeployObserver, ServiceRegistration<FileObserver>> observers = new ConcurrentHashMap<>();
    private BundleContext context;
    private Config config;

    // Constructor for OSGi DS
    public Hotdeployer() {
        this(new DirectoryFactory());
    }

    // Constructor for testing
    Hotdeployer(final DirectoryFactory pDirectoryFactory) {
        directoryFactory = pDirectoryFactory;
    }

    void setBundleDeterminator(final BundleDeterminator pBundleDeterminator) {
        bundleDeterminator = pBundleDeterminator;
    }

    void setResourceKeyFactory(final ResourceKeyFactory pResourceKeyFactory) {
        resourceKeyFactory = pResourceKeyFactory;
    }

    @Activate
    public void activate(final BundleContext pContext, final Config pConfig) throws IOException, URISyntaxException {
        context = pContext;
        config = pConfig;
        delegate = directoryFactory.newWatchedDirectory(pConfig);
        watchedDirectoryRegistration = pContext.registerService(WatchedDirectory.class, delegate, null);
        setBundleDeterminator(new BundleDeterminator(pContext, pConfig.bundleResourceDirectoryPrefix()));
        final ResourceKeyFactory resourceKeyFactory = new ResourceKeyFactory(bundleDeterminator);
        hookRegistration = pContext.registerService(KeyDeliveryHook.class, resourceKeyFactory, null);
        setResourceKeyFactory(resourceKeyFactory);
        LOG.info("Hotdeployer started");
    }

    @Modified
    public void modify(final Config pConfig) throws IOException, URISyntaxException {
        delegate.relocate(directoryFactory.getHotdeployDir(pConfig));
    }

    @Deactivate
    public void deactivate() {
        watchedDirectoryRegistration.unregister();
        hookRegistration.unregister();
        LOG.info("Hotdeployer shutdown");
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE)
    public void addObserver(final HotdeployObserver pObserver) {
        observers.put(pObserver, context.registerService(
                FileObserver.class,
                new FileObserverAdapter(resourceKeyFactory, pObserver),
                null));
    }

    public void removeObserver(final HotdeployObserver pObserver) {
        final ServiceRegistration<FileObserver> adapterRegistration = observers.remove(pObserver);
        if (adapterRegistration == null) {
            LOG.warn("No adapter was registered for hotdeployer-observer {}", pObserver);
        }
    }
}
