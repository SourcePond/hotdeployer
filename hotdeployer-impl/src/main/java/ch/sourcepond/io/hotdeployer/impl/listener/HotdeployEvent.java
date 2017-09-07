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
package ch.sourcepond.io.hotdeployer.impl.listener;

import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import ch.sourcepond.io.hotdeployer.api.FileChangeEvent;
import ch.sourcepond.io.hotdeployer.impl.key.DefaultResourceKey;
import org.osgi.framework.Bundle;

import java.nio.file.Path;

/**
 *
 */
class HotdeployEvent implements FileChangeEvent {
    private final PathChangeEvent delegate;
    private final DefaultResourceKey key;

    HotdeployEvent(final PathChangeEvent pDelegate, final DefaultResourceKey pKey) {
        delegate = pDelegate;
        key = pKey;
    }

    @Override
    public Bundle getSourceBundle() {
        return key.getDirectoryKey();
    }

    @Override
    public DispatchKey getKey() {
        return key;
    }

    @Override
    public Path getFile() {
        return delegate.getFile();
    }

    @Override
    public int getNumReplays() {
        return delegate.getNumReplays();
    }

    @Override
    public void replay() {
        delegate.replay();
    }

    @Override
    public String toString() {
        return "HotdeployEvent[key: " + getKey() + ", numReplays: " + getNumReplays() + ", file: " + getFile() + "]";
    }
}
