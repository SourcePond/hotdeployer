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

import ch.sourcepond.io.hotdeployer.api.ResourceKey;
import org.osgi.framework.Bundle;

import java.nio.file.Path;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

/**
 *
 */
final class DefaultResourceKey implements ResourceKey {
    private final Bundle source;
    private final Path relativePath;

    DefaultResourceKey(final Bundle pSource, final Path pRelativePath) {
        source = pSource;
        relativePath = pRelativePath;
    }

    @Override
    public Path getRelativePath() {
        return relativePath;
    }

    @Override
    public Bundle getSource() {
        return source;
    }

    @Override
    public boolean isParentKeyOf(final ResourceKey pOther) {
        requireNonNull(pOther, "Other key is null");
        return getSource().equals(pOther.getSource()) && pOther.getRelativePath().startsWith(getRelativePath());
    }

    @Override
    public boolean isSubKeyOf(final ResourceKey pOther) {
        requireNonNull(pOther, "Other key is null");
        return getSource().equals(pOther.getSource()) && getRelativePath().startsWith(pOther.getRelativePath());
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
                Objects.equals(relativePath, that.relativePath);
    }

    @Override
    public int hashCode() {
        return hash(source, relativePath);
    }

    @Override
    public String toString() {
        return format("[%s:%s]", source, relativePath);
    }
}
