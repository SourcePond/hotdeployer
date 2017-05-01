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

import org.osgi.framework.BundleContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *
 */
public class PostponeQueueFactory {
    private final ExecutorService postponeExecutor;

    // Constructor for activator
    public PostponeQueueFactory() {
        this(createDefaultExecutor());
    }

    // Constructor for testing
    PostponeQueueFactory(final ExecutorService pPostponeExecutor) {
        postponeExecutor = pPostponeExecutor;
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

    public PostponeQueue createQueue(final BundleContext pContext) {
        final PostponeQueue queue = new PostponeQueue(pContext);
        postponeExecutor.execute(queue);
        return queue;
    }
}
