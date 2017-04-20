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

import ch.sourcepond.io.fileobserver.api.SimpleDispatchRestriction;
import org.junit.Test;

import java.util.regex.Pattern;

import static ch.sourcepond.io.hotdeployer.impl.observer.DispatchRestrictionProxy.VERSION_RANGE_PATTERN;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class DispatchRestrictionProxyTest {
    private static final String ANY_PREFIX = "anyPrefix";
    private final SimpleDispatchRestriction restriction = mock(SimpleDispatchRestriction.class);
    private final DispatchRestrictionProxy proxy = new DispatchRestrictionProxy(ANY_PREFIX, restriction);

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
}
