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

import ch.sourcepond.io.hotdeployer.impl.Config;
import org.junit.Test;

import java.nio.file.FileSystem;

import static org.mockito.Mockito.*;

/**
 *
 */
public class ObserverAdapterFactoryTest {
    private static final String BUNDLE_RESOURCE_DIRECTORY_PREFIX = "prefix";
    private final DispatchEventProxyFactory eventProxyFactory = mock(DispatchEventProxyFactory.class);
    private final BundlePathDeterminator proxyFactory = mock(BundlePathDeterminator.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final Config config = mock(Config.class);
    private final ObserverAdapterFactory factory = new ObserverAdapterFactory(eventProxyFactory, proxyFactory);

    @Test
    public void setConfig() {
        when(config.bundleResourceDirectoryPrefix()).thenReturn(BUNDLE_RESOURCE_DIRECTORY_PREFIX);
        factory.setConfig(fs, config);
        verify(proxyFactory).setConfig(fs, BUNDLE_RESOURCE_DIRECTORY_PREFIX);
    }
}
