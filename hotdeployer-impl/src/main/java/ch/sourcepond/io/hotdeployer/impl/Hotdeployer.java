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

import ch.sourcepond.commons.smartswitch.api.SmartSwitchBuilderFactory;
import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import ch.sourcepond.io.hotdeployer.api.HotdeployObserver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
@Component
@Designate(ocd = Config.class)
public class Hotdeployer implements FileObserver {
    private static final Logger LOG = getLogger(Hotdeployer.class);
    private final Set<HotdeployObserver> observers = newKeySet();
    private final DirectoryFactory directoryFactory;
    private Executor observerExecutor;
    private WatchedDirectory delegate;
    private ServiceRegistration<WatchedDirectory> registration;

    // Constructor for OSGi DS
    public Hotdeployer() {
        this(new DirectoryFactory());
    }

    // Constructor for testing
    Hotdeployer(final DirectoryFactory pDirectoryFactory) {
        directoryFactory = pDirectoryFactory;
    }

    @Reference
    public void initExecutor(final SmartSwitchBuilderFactory pFactory) {
        observerExecutor = pFactory.newBuilder(ExecutorService.class).
                setFilter("(sourcepond.io.hotdeployer.observerexecutor=*)").
                setShutdownHook(ExecutorService::shutdown).
                build(Executors::newCachedThreadPool);
    }

    @Activate
    public void activate(final BundleContext pContext, final Config pConfig) throws IOException, URISyntaxException {
        delegate = directoryFactory.newWatchedDirectory(pConfig);
        registration = pContext.registerService(WatchedDirectory.class, delegate, null);
        LOG.info("Hotdeployer started");
    }

    @Modified
    public void modify(final Config pConfig) throws IOException, URISyntaxException {
        delegate.relocate(directoryFactory.getHotdeployDir(pConfig));
    }

    @Deactivate
    public void deactivate() {
        registration.unregister();
        LOG.info("Hotdeployer shutdown");
    }

    @Override
    public void supplement(final FileKey pKnownKey, final FileKey pAdditionalKey) {
        // noop because we are watching exactly one directory key. In this case
        // this method will never be called.
    }

    @Override
    public void modified(final FileKey fileKey, final Path path) {
        final Path relativePath = fileKey.relativePath();
        LOG.debug("Modified: relative-path : {} , absolute path {}", relativePath, path);
        observers.forEach(o -> observerExecutor.execute(() -> {
            try {
                o.modified(relativePath, path);
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        }));
    }

    @Override
    public void discard(final FileKey fileKey) {
        final Path relativePath = fileKey.relativePath();
        LOG.debug("Discard: {}", relativePath);
        observers.forEach(o ->
                observerExecutor.execute(() -> {
                    try {
                        o.discard(fileKey.relativePath());
                    } catch (final Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                }));
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE)
    public void addObserver(final HotdeployObserver pObserver) {
        observers.add(pObserver);
    }

    public void removeObserver(final HotdeployObserver pObserver) {
        observers.remove(pObserver);
    }
}
