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
import org.osgi.framework.Bundle;

import java.nio.file.Path;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.hash;

/**
 *
 */
final class DefaultResourceKey implements FileKey<Bundle> {
    private final FileKey fileKey;
    private final Bundle source;
    private final Path relativePath;

    DefaultResourceKey(final FileKey pFileKey,
                       final Bundle pSource,
                       final Path pRelativePath) {
        fileKey = pFileKey;
        source = pSource;
        relativePath = pRelativePath;
    }

    @Override
    public Path getRelativePath() {
        return relativePath;
    }

    @Override
    public Bundle getDirectoryKey() {
        return source;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefaultResourceKey that = (DefaultResourceKey) o;
        return Objects.equals(source, that.source) &&
                Objects.equals(fileKey, that.fileKey);
    }

    @Override
    public int hashCode() {
        return hash(source, fileKey);
    }

    @Override
    public String toString() {
        return format("[%s:%s]", source, relativePath);
    }
}
