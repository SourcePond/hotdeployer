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

import ch.sourcepond.commons.smartswitch.api.SmartSwitchBuilderFactory;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.nio.file.Path;

import static java.lang.System.getProperty;
import static java.nio.file.FileSystems.getDefault;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
@Ignore
public class HotdeployerTest {
    private final SmartSwitchBuilderFactory ssbBuilderFactory = mock(SmartSwitchBuilderFactory.class);
    private final Config config = mock(Config.class);
    private final BundleContext context = mock(BundleContext.class);
    private final ServiceRegistration<WatchedDirectory> registration = mock(ServiceRegistration.class);


    @Before
    public void setup() {

    }
}
