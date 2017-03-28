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
package ch.sourcepond.io.hotdeployer;

import ch.sourcepond.io.hotdeployer.api.HotdeployObserver;
import ch.sourcepond.testing.BundleContextClassLoaderRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;

import static ch.sourcepond.testing.OptionsHelper.karafContainer;
import static ch.sourcepond.testing.OptionsHelper.mockitoBundles;
import static java.lang.Thread.sleep;
import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.*;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

/**
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class HotdeployerTest {

    private static final Path TEST_DIR = getDefault().getPath(System.getProperty("user.dir"), "target", "hotdeploy");

    @Rule
    public BundleContextClassLoaderRule rule = new BundleContextClassLoaderRule(this);

    @Inject
    private BundleContext context;
    private final HotdeployObserver observer = mock(HotdeployObserver.class);
    private ServiceRegistration<HotdeployObserver> hotdeployObserverRegistration;
    private Path testFile;

    @Configuration
    public Option[] config() throws IOException {
        MavenUrlReference hotdeployerRepo = maven()
                .groupId("ch.sourcepond.io")
                .artifactId("hotdeployer-feature")
                .classifier("features")
                .type("xml")
                .versionAsInProject();

        return new Option[]{
                mavenBundle().groupId("ch.sourcepond.testing").artifactId("bundle-test-support").versionAsInProject(),
                mockitoBundles(),
                editConfigurationFilePut("etc/ch.sourcepond.io.hotdeployer.impl.Hotdeployer.cfg", "hotdeployDirectoryURI", createDirectories(TEST_DIR).toUri().toString()),
                karafContainer(features(hotdeployerRepo, "hotdeployer-feature"))
        };
    }

    private void writeArbitraryContent() throws Exception {
        try (final BufferedWriter writer = newBufferedWriter(testFile, CREATE)) {
            writer.write(randomUUID().toString());
        }
        sleep(500);
    }

    @Before
    public void setup() throws Exception {
        testFile = TEST_DIR.resolve("test.txt");
        hotdeployObserverRegistration = context.registerService(HotdeployObserver.class, observer, null);
    }

    @After
    public void tearDown() throws InterruptedException {
        hotdeployObserverRegistration.unregister();
    }

    @Test
    public void verifyModifyAndDiscard() throws Exception {
        // Write every 500ms something into file; do this 10 times
        for (int i = 0 ; i < 10 ; i++) {
            writeArbitraryContent();
        }

        // modified should have been called exactly once
        final Path relativePath = TEST_DIR.relativize(testFile);
        verify(observer, timeout(20000)).modified(relativePath, testFile);

        delete(testFile);
        verify(observer, timeout(10000)).discard(relativePath);
    }
}
