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

import org.junit.Test;
import org.osgi.framework.BundleContext;

import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 */
public class PostponeQueueFactoryTest {
    private final ExecutorService postponeExecutor = mock(ExecutorService.class);
    private final BundleContext context = mock(BundleContext.class);
    private final PostponeQueue queue = new PostponeQueueFactory(postponeExecutor).createQueue(context);

    @Test
    public void verifyDefaultConstructor() {
        new PostponeQueueFactory().shutdown();
    }

    @Test
    public void startQueue() {
        assertNotNull(queue);
        verify(postponeExecutor).execute(queue);
    }
}
