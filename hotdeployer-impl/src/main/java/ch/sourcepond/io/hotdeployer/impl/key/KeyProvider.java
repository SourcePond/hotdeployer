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
package ch.sourcepond.io.hotdeployer.impl.key;

import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.KeyDeliveryHook;
import ch.sourcepond.io.hotdeployer.impl.determinator.BundleDeterminator;
import ch.sourcepond.io.hotdeployer.impl.determinator.BundleNotAvailableException;
import org.osgi.framework.Bundle;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static ch.sourcepond.io.hotdeployer.impl.determinator.BundleDeterminator.SPECIAL_BUNDLE_NAME_COUNT;
import static java.lang.String.format;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;

/**
 *
 */
public class KeyProvider implements KeyDeliveryHook {
    private final ConcurrentMap<DispatchKey, Object> keys = new ConcurrentHashMap<>();
    private final BundleDeterminator determinator;

    KeyProvider(final BundleDeterminator pDeterminator) {
        determinator = pDeterminator;
    }

    private Path adjustRelativePath(final Bundle pBundle, final Path pRelativePath) {
        final Path adjustedRelativePath;
        if (SYSTEM_BUNDLE_ID == pBundle.getBundleId()) {
            adjustedRelativePath = pRelativePath;
        } else {
            adjustedRelativePath = pRelativePath.subpath(SPECIAL_BUNDLE_NAME_COUNT, pRelativePath.getNameCount());
        }
        return adjustedRelativePath;
    }

    @Override
    public void before(final DispatchKey pKey) {
        try {
            final Bundle bundle = determinator.determine(pKey.getRelativePath());
            keys.put(pKey, new DefaultResourceKey(pKey,
                    bundle,
                    adjustRelativePath(bundle, pKey.getRelativePath())));
        } catch (final Exception e) {
            keys.put(pKey, e);
        }
    }

    @Override
    public void after(final DispatchKey pKey) {
        keys.remove(pKey);
    }

    @Override
    public void afterDiscard(final DispatchKey pKey) {
        after(pKey);
        determinator.clearCacheFor(pKey.getRelativePath());
    }

    public DefaultResourceKey getKey(final DispatchKey pKey) throws ResourceKeyException {
        final Object keyOrException = keys.get(pKey);

        if (keyOrException instanceof BundleNotAvailableException) {
            throw (BundleNotAvailableException)keyOrException;
        }

        if (keyOrException instanceof Exception) {
            throw new ResourceKeyException(format("File-key %s could not be adapted to a resource-key!", pKey), (Exception)keyOrException);
        }
        return (DefaultResourceKey) keyOrException;
    }
}
