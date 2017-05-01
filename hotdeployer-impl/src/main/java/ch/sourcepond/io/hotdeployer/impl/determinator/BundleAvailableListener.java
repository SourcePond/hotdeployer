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

import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.osgi.framework.BundleEvent.RESOLVED;

/**
 *
 */
class BundleAvailableListener implements BundleListener, Delayed {
    private final BlockingQueue<BundleAvailableListener> queue;
    private final BundleContext context;
    private final PathChangeEvent pathChangeEvent;
    private final BundleNotAvailableException cause;
    private final Long dueTimeInMillis;

    BundleAvailableListener(final BlockingQueue<BundleAvailableListener> pQueue,
                            final BundleContext pContext,
                            final PathChangeEvent pEvent,
                            final BundleNotAvailableException pCause,
                            final long pDueTimeInMillis) {
        queue = pQueue;
        context = pContext;
        pathChangeEvent = pEvent;
        cause = pCause;
        dueTimeInMillis = pDueTimeInMillis;
    }

    public boolean tryReplay(final Bundle pBundle) {
        if (cause.getSymbolicName().equals(pBundle.getSymbolicName()) &&
                cause.getVersionRange().includes(pBundle.getVersion()) &&
                queue.remove(this)) {
            context.removeBundleListener(this);
            pathChangeEvent.replay();
            return true;
        }
        return false;
    }


    PathChangeEvent getEvent() {
        return pathChangeEvent;
    }

    @Override
    public void bundleChanged(final BundleEvent event) {
        if (RESOLVED == event.getType()) {
            tryReplay(event.getBundle());
        }
    }

    @Override
    public long getDelay(final TimeUnit unit) {
        return unit.convert(dueTimeInMillis.longValue() - now().toEpochMilli(), MILLISECONDS);
    }

    @Override
    public int compareTo(final Delayed o) {
        return dueTimeInMillis.compareTo(((BundleAvailableListener) o).dueTimeInMillis);
    }

    public BundleNotAvailableException getException() {
        return cause;
    }
}
