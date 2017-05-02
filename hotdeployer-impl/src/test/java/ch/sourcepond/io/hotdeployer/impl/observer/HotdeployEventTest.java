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

import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import ch.sourcepond.io.hotdeployer.impl.key.DefaultResourceKey;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

import java.nio.file.Path;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class HotdeployEventTest {
    private final Bundle sourceBundle = mock(Bundle.class);
    private final PathChangeEvent event = mock(PathChangeEvent.class);
    private final DefaultResourceKey key = mock(DefaultResourceKey.class);
    private final HotdeployEvent proxy = new HotdeployEventFactory().create(event, key);

    @Before
    public void setup() {
        when(key.getDirectoryKey()).thenReturn(sourceBundle);
    }

    @Test
    public void getSourceBundle() {
        assertSame(sourceBundle, proxy.getSourceBundle());
    }

    @Test
    public void getKey() {
        assertSame(key, proxy.getKey());
    }

    @Test
    public void getFile() {
        final Path file = mock(Path.class);
        when(event.getFile()).thenReturn(file);
        assertSame(file, proxy.getFile());
    }

    @Test
    public void getNumReplays() {
        when(event.getNumReplays()).thenReturn(10);
        assertEquals(10, proxy.getNumReplays());
    }

    @Test
    public void replay() {
        proxy.replay();
        verify(event).replay();
    }

    @Test
    public void verifyToString() {
        // Should not throw an exception
        proxy.toString();
    }
}
