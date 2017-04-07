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
package ch.sourcepond.io.hotdeployer.impl;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.hotdeployer.api.HotdeployObserver;
import ch.sourcepond.io.hotdeployer.api.ResourceKey;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class FileObserverAdapter implements FileObserver {
    private static final Logger LOG = getLogger(FileObserverAdapter.class);
    private final ResourceKeyFactory resourceKeyFactory;
    private final HotdeployObserver hotdeployObserver;

    FileObserverAdapter(final ResourceKeyFactory pResourceKeyFactory,
                        final HotdeployObserver pHotdeployObserver) {
        resourceKeyFactory = pResourceKeyFactory;
        hotdeployObserver = pHotdeployObserver;
    }

    @Override
    public void supplement(final FileKey pKnownKey, final FileKey pAdditionalKey) {
        // noop because we are watching exactly one directory key. In this case
        // this method will never be called.
    }

    @Override
    public void modified(final FileKey fileKey, final Path path) throws IOException {
        final ResourceKey key = resourceKeyFactory.getKey(fileKey);
        hotdeployObserver.modified(key, path);
        LOG.debug("Modified: resource-key : {} , absolute path {}", key, path);
    }

    @Override
    public void discard(final FileKey fileKey) {
        final ResourceKey key = resourceKeyFactory.getKey(fileKey);
        hotdeployObserver.discard(key);
        LOG.debug("Discard: resource-key : {}", key);
    }
}
