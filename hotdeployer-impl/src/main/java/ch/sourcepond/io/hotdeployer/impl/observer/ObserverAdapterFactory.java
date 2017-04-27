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
import ch.sourcepond.io.hotdeployer.impl.key.KeyProvider;

import java.nio.file.FileSystem;

/**
 *
 */
public class ObserverAdapterFactory {
    private final BundlePathDeterminator proxyFactory;

    // Constructor for activator
    public ObserverAdapterFactory() {
        this(new BundlePathDeterminator());
    }


    // Constructor for testing
    ObserverAdapterFactory(final BundlePathDeterminator pProxyFactory) {
        proxyFactory = pProxyFactory;
    }

    public void setConfig(final FileSystem pFileSystem, final Config pConfig) {
        proxyFactory.setConfig(pFileSystem, pConfig.bundleResourceDirectoryPrefix());
    }

    public PathChangeListener createAdapter(final KeyProvider pKeyProvider,
                                            final FileChangeListener pFileChangeListener) {
        return new ObserverAdapter(proxyFactory, pKeyProvider, pFileChangeListener);
    }
}
