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
import ch.sourcepond.io.fileobserver.api.KeyDeliveryHook;
import ch.sourcepond.io.hotdeployer.api.ResourceKey;
import org.osgi.framework.Bundle;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static ch.sourcepond.io.hotdeployer.impl.BundleDeterminator.SPECIAL_BUNDLE_NAME_COUNT;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;

/**
 *
 */
class KeyProvider implements KeyDeliveryHook {
    private final ConcurrentMap<FileKey, ResourceKey> keys = new ConcurrentHashMap<>();
    private final BundleDeterminator determinator;

    KeyProvider(final BundleDeterminator pDeterminator) {
        determinator = pDeterminator;
    }

    private Path adjustRelativePath(final Bundle pBundle, final Path pRelativePath) {
        final Path adjustedRelativePath;
        final Bundle bundle = determinator.determine(pRelativePath);
        if (SYSTEM_BUNDLE_ID == bundle.getBundleId()) {
            adjustedRelativePath = pRelativePath;
        } else {
            adjustedRelativePath = pRelativePath.getName(SPECIAL_BUNDLE_NAME_COUNT);
        }
        return adjustedRelativePath;
    }

    @Override
    public void before(final FileKey pKey) {
        final Bundle bundle = determinator.determine(pKey.relativePath());
        final ResourceKey key = new DefaultResourceKey(bundle, adjustRelativePath(bundle, pKey.relativePath()));
        keys.put(pKey, key);
    }

    @Override
    public void after(final FileKey pKey) {
        keys.remove(pKey);
    }

    ResourceKey getKey(final FileKey pKey) {
        return keys.get(pKey);
    }
}
