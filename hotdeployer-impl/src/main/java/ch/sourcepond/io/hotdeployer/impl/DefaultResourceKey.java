package ch.sourcepond.io.hotdeployer.impl;

import ch.sourcepond.io.hotdeployer.api.ResourceKey;
import org.osgi.framework.Bundle;

import java.nio.file.Path;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

/**
 * Created by rolandhauser on 04.04.17.
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
    public boolean isSubKeyOf(final ResourceKey pOther) {
        requireNonNull(pOther);
        return getSource().equals(pOther.getSource()) && pOther.getRelativePath().startsWith(pOther.getRelativePath());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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
