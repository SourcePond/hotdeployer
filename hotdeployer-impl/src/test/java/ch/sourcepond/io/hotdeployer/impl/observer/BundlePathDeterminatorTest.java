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

import ch.sourcepond.io.fileobserver.api.DispatchRestriction;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import static ch.sourcepond.io.hotdeployer.impl.observer.BundlePathDeterminator.escape;
import static java.lang.System.getProperty;
import static java.nio.file.FileSystems.getDefault;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 *
 */
public class BundlePathDeterminatorTest {
    public static final String PREFIX = "$BUNDLE$_";
    private final FileSystem fs = getDefault();
    private final Path root = fs.getPath(getProperty("user.dir"));
    private final PathMatcher matcher = mock(PathMatcher.class);
    private final Bundle bundle = mock(Bundle.class);
    private BundlePathDeterminator determinator = new BundlePathDeterminator();

    @Before
    public void setup() {
        when(bundle.getVersion()).thenReturn(Version.valueOf("1.0.0"));
        when(matcher.matches(argThat(p -> p.toString().equals("test.xml")))).thenReturn(true);
        determinator.setConfig(fs, "$BUNDLE$_");
    }

    private Path toRelativePath(final String pBundle, final String pVersionRange) {
        final Path file = root.resolve(pBundle).resolve(pVersionRange).resolve("test.xml");
        return root.relativize(file);
    }

    private void assertMatch(final String pBundle, final String pVersionRange) {
        assertTrue(determinator.apply(matcher, toRelativePath(pBundle, pVersionRange)));
    }

    private void assertNoMatch(final String pBundle, final String pVersionRange) {
        assertFalse(determinator.apply(matcher, toRelativePath(pBundle, pVersionRange)));
    }

    @Test
    public void createDispatchRestrictionProxy() {
        final DispatchRestriction restriction = mock(DispatchRestriction.class);
        DispatchRestrictionProxy proxy = determinator.createProxy(restriction);
        proxy.addPathMatcher(matcher);
        verify(restriction).addPathMatcher((PathMatcher) argThat(m -> BundlePathMatcher.class.equals(m.getClass())));
    }

    @Test
    public void verifyBundlePathMatcher() {
        final Path path = toRelativePath("$BUNDLE$_com.foo.bar", "1.0.0");
        when(matcher.matches(path)).thenReturn(true);
        final BundlePathMatcher bundlePathMatcher = determinator.create(matcher);
        assertTrue(bundlePathMatcher.matches(path));
    }

    @Test
    public void verifyVersionRangePattern() {
        assertMatch("$BUNDLE$_com.foo.bar", "1.0.0");
        assertMatch("$BUNDLE$_com.foo.bar", "[1.0.0, ]");
        assertMatch("$BUNDLE$_com.foo.bar", "[1.0.0,2.0.0]");
        assertMatch("$BUNDLE$_com.foo.bar", "[1.0.0, 2.0.0]");
        assertMatch("$BUNDLE$_com.foo.bar", "[1.0.0, )");
        assertMatch("$BUNDLE$_com.foo.bar", "[1.0.0,2.0.0)");
        assertMatch("$BUNDLE$_com.foo.bar", "[1.0.0, 2.0.0)");
        assertMatch("$BUNDLE$_com.foo.bar", "(1.0.0, )");
        assertMatch("$BUNDLE$_com.foo.bar", "(1.0.0,2.0.0)");
        assertMatch("$BUNDLE$_com.foo.bar", "(1.0.0, 2.0.0)");
        assertMatch("$BUNDLE$_com.foo.bar", "(1.0.0, ]");
        assertMatch("$BUNDLE$_com.foo.bar", "(1.0.0,2.0.0]");
        assertMatch("$BUNDLE$_com.foo.bar", "(1.0.0, 2.0.0]");

        assertMatch("$BUNDLE$_com.foo.bar", "1.0.0.v2017_10-10CND");
        assertMatch("$BUNDLE$_com.foo.bar", "[1.0.0.v2017_10-10CND, ]");
        assertMatch("$BUNDLE$_com.foo.bar", "[1.0.0.v2017_10-10CND,2.0.0.v2017_10-10CND]");
        assertMatch("$BUNDLE$_com.foo.bar", "[1.0.0.v2017_10-10CND, 2.0.0.v2017_10-10CND]");
        assertMatch("$BUNDLE$_com.foo.bar", "[1.0.0.v2017_10-10CND, )");
        assertMatch("$BUNDLE$_com.foo.bar", "[1.0.0.v2017_10-10CND,2.0.0.v2017_10-10CND)");
        assertMatch("$BUNDLE$_com.foo.bar", "[1.0.0.v2017_10-10CND, 2.0.0.v2017_10-10CND)");
        assertMatch("$BUNDLE$_com.foo.bar", "(1.0.0.v2017_10-10CND, )");
        assertMatch("$BUNDLE$_com.foo.bar", "(1.0.0.v2017_10-10CND,2.0.0.v2017_10-10CND)");
        assertMatch("$BUNDLE$_com.foo.bar", "(1.0.0.v2017_10-10CND, 2.0.0.v2017_10-10CND)");
        assertMatch("$BUNDLE$_com.foo.bar", "(1.0.0.v2017_10-10CND, ]");
        assertMatch("$BUNDLE$_com.foo.bar", "(1.0.0.v2017_10-10CND,2.0.0.v2017_10-10CND]");
        assertMatch("$BUNDLE$_com.foo.bar", "(1.0.0.v2017_10-10CND, 2.0.0.v2017_10-10CND]");

        assertNoMatch("$BUNDLE$_com.foo.bar", "V1.0.0");
        assertNoMatch("$BUNDLE$_com.foo.bar", "[V1.0.0, ]");
        assertNoMatch("$BUNDLE$_com.foo.bar", "[V1.0.0,2.0.0]");
        assertNoMatch("$BUNDLE$_com.foo.bar", "[1.0.0, V2.0.0]");
        assertNoMatch("$BUNDLE$_com.foo.bar", "[1.0.0,");
        assertNoMatch("$BUNDLE$_com.foo.bar", "1.0.0,2.0.0)");
        assertNoMatch("$BUNDLE$_com.foo.bar", "1.0.0,2.0.0");
        assertNoMatch("$BUNDLE$_com.foo.bar", "1.0.0, 2.0.0");

        assertNoMatch("com.foo.bar", "1.0.0");
        assertNoMatch("BUNDLE_com.foo.bar", "1.0.0");
    }

    @Test
    public void verifyEscapePrefixPattern() {
        assertEquals("T\\\\EST", escape("T\\EST"));
        assertEquals("TE\\^ST", escape("TE^ST"));
        assertEquals("\\$TEST", escape("$TEST"));
        assertEquals("TES\\.T", escape("TES.T"));
        assertEquals("T\\|EST", escape("T|EST"));
        assertEquals("TEST\\?", escape("TEST?"));
        assertEquals("\\*TEST", escape("*TEST"));
        assertEquals("TE\\+ST", escape("TE+ST"));
        assertEquals("TES\\(T", escape("TES(T"));
        assertEquals("TES\\)T", escape("TES)T"));
        assertEquals("T\\[EST", escape("T[EST"));
        assertEquals("T\\{EST", escape("T{EST"));
    }
}
