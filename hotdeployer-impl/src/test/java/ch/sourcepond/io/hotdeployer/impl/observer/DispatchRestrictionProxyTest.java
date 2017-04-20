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

import ch.sourcepond.io.fileobserver.api.PathMatcherBuilder;
import ch.sourcepond.io.fileobserver.api.SimpleDispatchRestriction;
import org.junit.Test;

import java.nio.file.PathMatcher;
import java.util.regex.Pattern;

import static ch.sourcepond.io.hotdeployer.impl.observer.DispatchRestrictionProxy.VERSION_RANGE_PATTERN;
import static ch.sourcepond.io.hotdeployer.impl.observer.DispatchRestrictionProxy.escape;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DispatchRestrictionProxyTest {
    private static final String ANY_PREFIX = "anyPrefix";
    private static final String ANY_SYNTAX = "anySyntax";
    private static final String ANY_PATTERN = "anyPattern";
    private final PathMatcherBuilder builder = mock(PathMatcherBuilder.class);
    private final SimpleDispatchRestriction delegate = mock(SimpleDispatchRestriction.class);
    private final DispatchRestrictionProxyFactory factory = new DispatchRestrictionProxyFactory();
    private final DispatchRestrictionProxy proxy = factory.createProxy(ANY_PREFIX, delegate);

    @Test
    public void verifyVersionRangePattern() {
        final Pattern pattern = compile(VERSION_RANGE_PATTERN);
        assertTrue(pattern.matcher("1.0.0").matches());
        assertTrue(pattern.matcher("[1.0.0,]").matches());
        assertTrue(pattern.matcher("[1.0.0,2.0.0]").matches());
        assertTrue(pattern.matcher("[1.0.0, 2.0.0]").matches());
        assertTrue(pattern.matcher("[1.0.0,)").matches());
        assertTrue(pattern.matcher("[1.0.0,2.0.0)").matches());
        assertTrue(pattern.matcher("[1.0.0, 2.0.0)").matches());
        assertTrue(pattern.matcher("(1.0.0,)").matches());
        assertTrue(pattern.matcher("(1.0.0,2.0.0)").matches());
        assertTrue(pattern.matcher("(1.0.0, 2.0.0)").matches());
        assertTrue(pattern.matcher("(1.0.0,]").matches());
        assertTrue(pattern.matcher("(1.0.0,2.0.0]").matches());
        assertTrue(pattern.matcher("(1.0.0, 2.0.0]").matches());

        assertTrue(pattern.matcher("1.0.0.v2017_10-10CND").matches());
        assertTrue(pattern.matcher("[1.0.0.v2017_10-10CND,]").matches());
        assertTrue(pattern.matcher("[1.0.0.v2017_10-10CND,2.0.0.v2017_10-10CND]").matches());
        assertTrue(pattern.matcher("[1.0.0.v2017_10-10CND, 2.0.0.v2017_10-10CND]").matches());
        assertTrue(pattern.matcher("[1.0.0.v2017_10-10CND,)").matches());
        assertTrue(pattern.matcher("[1.0.0.v2017_10-10CND,2.0.0.v2017_10-10CND)").matches());
        assertTrue(pattern.matcher("[1.0.0.v2017_10-10CND, 2.0.0.v2017_10-10CND)").matches());
        assertTrue(pattern.matcher("(1.0.0.v2017_10-10CND,)").matches());
        assertTrue(pattern.matcher("(1.0.0.v2017_10-10CND,2.0.0.v2017_10-10CND)").matches());
        assertTrue(pattern.matcher("(1.0.0.v2017_10-10CND, 2.0.0.v2017_10-10CND)").matches());
        assertTrue(pattern.matcher("(1.0.0.v2017_10-10CND,]").matches());
        assertTrue(pattern.matcher("(1.0.0.v2017_10-10CND,2.0.0.v2017_10-10CND]").matches());
        assertTrue(pattern.matcher("(1.0.0.v2017_10-10CND, 2.0.0.v2017_10-10CND]").matches());

        assertFalse(pattern.matcher("V1.0.0").matches());
        assertFalse(pattern.matcher("[V1.0.0,]").matches());
        assertFalse(pattern.matcher("[V1.0.0,2.0.0]").matches());
        assertFalse(pattern.matcher("[1.0.0, V2.0.0]").matches());
        assertFalse(pattern.matcher("[1.0.0,").matches());
        assertFalse(pattern.matcher("1.0.0,2.0.0)").matches());
        assertFalse(pattern.matcher("1.0.0,2.0.0").matches());
        assertFalse(pattern.matcher("1.0.0, 2.0.0").matches());
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

    @Test
    public void whenPathMatchesPattern() {
        when(delegate.whenPathMatchesRegex("^anyPrefix.*")).thenReturn(builder);
        when(builder.andRegex(VERSION_RANGE_PATTERN)).thenReturn(builder);
        when(delegate.whenPathMatchesPattern(ANY_SYNTAX, ANY_PATTERN)).thenReturn(builder);

        assertSame(builder, proxy.whenPathMatchesPattern(ANY_SYNTAX, ANY_PATTERN));
        verify(builder).andPattern(ANY_SYNTAX, ANY_PATTERN);
    }

    @Test
    public void whenPathMatches() {
        final PathMatcher matcher = mock(PathMatcher.class);
        when(delegate.whenPathMatches(matcher)).thenReturn(builder);
        assertSame(builder, proxy.whenPathMatches(matcher));
    }
}
