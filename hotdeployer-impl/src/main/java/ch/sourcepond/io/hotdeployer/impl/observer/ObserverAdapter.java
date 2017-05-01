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

import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.DispatchRestriction;
import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import ch.sourcepond.io.hotdeployer.api.FileChangeListener;
import ch.sourcepond.io.hotdeployer.impl.determinator.BundleNotAvailableException;
import ch.sourcepond.io.hotdeployer.impl.determinator.PostponeQueue;
import ch.sourcepond.io.hotdeployer.impl.key.KeyProvider;
import ch.sourcepond.io.hotdeployer.impl.key.ResourceKeyException;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystem;

import static ch.sourcepond.io.hotdeployer.impl.DirectoryFactory.DIRECTORY_KEY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class ObserverAdapter implements PathChangeListener {
    private static final Logger LOG = getLogger(ObserverAdapter.class);
    private final PostponeQueue queue;
    private final BundleContext context;
    private final HotdeployEventFactory eventProxyFactory;
    private final BundlePathDeterminator proxyFactory;
    private final KeyProvider keyProvider;
    private final FileChangeListener fileChangeListener;

    ObserverAdapter(final PostponeQueue pQueue,
                    final BundleContext pContext,
                    final HotdeployEventFactory pEventProxyFactory,
                    final BundlePathDeterminator pProxyFactory,
                    final KeyProvider pKeyProvider,
                    final FileChangeListener pFileChangeListener) {
        queue = pQueue;
        context = pContext;
        eventProxyFactory = pEventProxyFactory;
        proxyFactory = pProxyFactory;
        keyProvider = pKeyProvider;
        fileChangeListener = pFileChangeListener;
    }

    @Override
    public void restrict(final DispatchRestriction pRestriction, final FileSystem pFileSystem) {
        pRestriction.accept(DIRECTORY_KEY);
        fileChangeListener.restrict(proxyFactory.createProxy(pRestriction, context.getBundle()), pFileSystem);
    }

    @Override
    public void supplement(final DispatchKey pKnownKey, final DispatchKey pAdditionalKey) {
        // noop because we are watching exactly one directory key. In this case
        // this method will never be called.
    }

    @Override
    public void modified(final PathChangeEvent pEvent) throws IOException {
        try {
            final DispatchKey key = keyProvider.getKey(pEvent.getKey());
            fileChangeListener.modified(eventProxyFactory.create(pEvent, key));
            LOG.debug("Modified: {}", pEvent);
        } catch (final BundleNotAvailableException e) {
            queue.postpone(context, pEvent, e);
        } catch (final ResourceKeyException e) {
            LOG.warn("Observer was not informed about modification because a problem was reported!", e);
        }
    }

    @Override
    public void discard(final DispatchKey pKey) {
        try {
            final DispatchKey key = keyProvider.getKey(pKey);
            fileChangeListener.discard(key);
            LOG.debug("Discard: resource-key : {}", key);
        } catch (final BundleNotAvailableException e) {
            queue.dropEvents(pKey);
        } catch (final ResourceKeyException e) {
            LOG.warn("Observer was not informed about discard because a problem was reported!", e);
        }
    }
}
