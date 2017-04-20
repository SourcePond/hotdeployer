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
import org.junit.Test;
import org.osgi.framework.Bundle;

import java.nio.file.Path;

import static java.lang.String.format;
import static java.util.Objects.hash;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DefaultResourceKeyTest {
    private final Bundle firstBundle = mock(Bundle.class);
    private final Bundle secondBundle = mock(Bundle.class);
    private final Path firstPath = mock(Path.class, withSettings().name("root"));
    private final Path secondPath = mock(Path.class);
    private final FileKey firstFileKey = mock(FileKey.class);
    private final FileKey secondFileKey = mock(FileKey.class);
    private final DefaultResourceKey firstKey = new DefaultResourceKey(firstFileKey, firstBundle, firstPath);
    private DefaultResourceKey secondKey = new DefaultResourceKey(secondFileKey, secondBundle, secondPath);

    @Test
    public void getRelativePath() {
        assertSame(firstPath, firstKey.getRelativePath());
    }

    @Test
    public void getSource() {
        assertSame(firstBundle, firstKey.getDirectoryKey());
    }

    @Test
    public void verifyEquals() {
        assertTrue(firstKey.equals(firstKey));
        assertFalse(firstKey.equals(null));
        assertFalse(firstKey.equals(new Object()));
        assertFalse(firstKey.equals(secondKey));
        secondKey = new DefaultResourceKey(secondFileKey, secondBundle, firstPath);
        assertFalse(firstKey.equals(secondKey));
        secondKey = new DefaultResourceKey(secondFileKey, firstBundle, secondPath);
        assertFalse(firstKey.equals(secondKey));
        secondKey = new DefaultResourceKey(firstFileKey, firstBundle, firstPath);
        assertTrue(firstKey.equals(secondKey));
    }

    @Test
    public void verifyHash() {
        assertEquals(hash(firstBundle, firstFileKey), firstKey.hashCode());
    }

    @Test
    public void verifyToString() {
        assertEquals(format("[%s:%s]", firstBundle, firstPath), firstKey.toString());
    }
}
