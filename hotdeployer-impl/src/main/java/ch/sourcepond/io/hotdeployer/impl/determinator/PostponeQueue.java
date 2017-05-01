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
package ch.sourcepond.io.hotdeployer.impl.determinator;

import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import ch.sourcepond.io.hotdeployer.impl.Config;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

import static java.lang.Thread.currentThread;
import static java.time.Duration.ofMillis;
import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public class PostponeQueue implements Runnable {
    private static final Logger LOG = getLogger(PostponeQueue.class);
    private final BlockingQueue<BundleAvailableListener> queue;
    private final BundleContext context;
    private final Logger logger;
    private Config config;

    // Constructor for activator
    PostponeQueue(final BundleContext pContext) {
        this(pContext, new DelayQueue<>(), LOG);
    }

    // Constructor for testing
    PostponeQueue(final BundleContext pContext,
                  final BlockingQueue<BundleAvailableListener> pQueue,
                  final Logger pLogger) {
        context = pContext;
        queue = pQueue;
        logger = pLogger;
    }

    public void setConfig(final Config pConfig) {
        config = pConfig;
    }

    public void dropEvents(final DispatchKey pKey) {
        queue.removeIf(e -> {
            final boolean remove = pKey.equals(e.getEvent().getKey());
            if (remove) {
                LOG.warn("Dropped {} from postpone queue because key was discarded.", e);
            }
            return remove;
        });
    }

    private void enqueIfNecessary(final BundleAvailableListener pListener) {
        for (final Bundle bundle : context.getBundles()) {
            if (pListener.tryReplay(bundle)) {
                return;
            }
        }
        queue.offer(pListener);
    }

    public void postpone(final PathChangeEvent pEvent, final BundleNotAvailableException pCause) {
        final long timeout = MILLISECONDS.convert(config.bundleAvailabilityTimeout(),
                config.bundleAvailabilityTimeoutUnit());

        if (logger.isDebugEnabled()) {
            logger.debug("Postponed with timout of {} ms: {}", timeout, pEvent);
        }

        final BundleAvailableListener listener = new BundleAvailableListener(queue, context, pEvent, pCause,
                now().plus(ofMillis(timeout)).toEpochMilli());
        context.addBundleListener(listener);

        // In case the bundle just got available during listener registration...
        enqueIfNecessary(listener);
    }

    @Override
    public void run() {
        while (!currentThread().isInterrupted()) {
            try {
                final BundleAvailableListener l = queue.take();
                logger.warn("Postponed dispatch of {} failed because timeout! Reason of postpone was: ", l.getEvent(), l.getException());
            } catch (final InterruptedException e) {
                currentThread().interrupt();
            }
        }
    }
}
