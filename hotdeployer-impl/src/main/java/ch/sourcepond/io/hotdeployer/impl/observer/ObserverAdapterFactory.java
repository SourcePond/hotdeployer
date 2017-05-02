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

import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import ch.sourcepond.io.hotdeployer.api.FileChangeListener;
import ch.sourcepond.io.hotdeployer.impl.Config;
import ch.sourcepond.io.hotdeployer.impl.determinator.PostponeQueue;
import ch.sourcepond.io.hotdeployer.impl.key.KeyProvider;
import org.osgi.framework.BundleContext;

import java.nio.file.FileSystem;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *
 */
public class ObserverAdapterFactory {
    private final ExecutorService postponeExecutor;
    private final PostponeQueue queue;
    private final HotdeployEventFactory eventProxyFactory;
    private final BundlePathDeterminator proxyFactory;

    // Constructor for activator
    public ObserverAdapterFactory(final BundleContext pContext) {
        this(createDefaultExecutor(),
                new PostponeQueue(pContext),
                new HotdeployEventFactory(),
                new BundlePathDeterminator());
    }

    // Constructor for testing
    ObserverAdapterFactory(final ExecutorService pPostponeExecutor,
                           final PostponeQueue pQueue,
                           final HotdeployEventFactory pEventProxyFactory,
                           final BundlePathDeterminator pProxyFactory) {
        postponeExecutor = pPostponeExecutor;
        queue = pQueue;
        eventProxyFactory = pEventProxyFactory;
        proxyFactory = pProxyFactory;
        postponeExecutor.execute(queue);
    }

    static ThreadPoolExecutor createDefaultExecutor() {
        final ThreadPoolExecutor tp = new ThreadPoolExecutor(1,
                1,
                60L,
                SECONDS,
                new LinkedBlockingQueue<>());
        tp.allowCoreThreadTimeOut(true);
        return tp;
    }

    public void shutdown() {
        postponeExecutor.shutdown();
    }

    public void setConfig(final FileSystem pFileSystem, final Config pConfig) {
        proxyFactory.setConfig(pFileSystem, pConfig.bundleResourceDirectoryPrefix());
        queue.setBundleAvailabilityTimeout(pConfig.bundleAvailabilityTimeout(), pConfig.bundleAvailabilityTimeoutUnit());
    }

    public PathChangeListener createAdapter(final BundleContext pContext,
                                            final KeyProvider pKeyProvider,
                                            final FileChangeListener pFileChangeListener) {
        return new ObserverAdapter(queue,
                pContext,
                eventProxyFactory,
                proxyFactory,
                pKeyProvider,
                pFileChangeListener);
    }
}
