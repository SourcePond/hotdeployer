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
import ch.sourcepond.io.fileobserver.api.PathChangeEvent;

import java.nio.file.Path;

/**
 *
 */
class DispatchEventProxy implements PathChangeEvent {
    private final PathChangeEvent delegate;
    private final DispatchKey key;

    DispatchEventProxy(final PathChangeEvent pDelegate, final DispatchKey pKey) {
        delegate = pDelegate;
        key = pKey;
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
}
