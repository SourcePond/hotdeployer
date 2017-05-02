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

import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.SimpleDispatchRestriction;
import ch.sourcepond.io.hotdeployer.api.FileChangeEvent;
import ch.sourcepond.io.hotdeployer.api.FileChangeListener;
import ch.sourcepond.testing.BundleContextClassLoaderRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static ch.sourcepond.testing.OptionsHelper.karafContainer;
import static ch.sourcepond.testing.OptionsHelper.mockitoBundles;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.Thread.sleep;
import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;

/**
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class HotdeployerTest {
    private final static Path TEST_DIR = getDefault().getPath(getProperty("user.dir"), "target", "hotdeploy");

    private static class TestListener implements FileChangeListener {
        final FileChangeListener mock = mock(FileChangeListener.class);

        @Override
        public void restrict(final SimpleDispatchRestriction pRestriction, final FileSystem pFileSystem) {
            mock.restrict(pRestriction, pFileSystem);
            pRestriction.addPathMatcher("glob:**/*");
        }

        @Override
        public void modified(final FileChangeEvent pEvent) throws IOException {
            mock.modified(pEvent);
        }

        @Override
        public void discard(final DispatchKey pKey) {
            mock.discard(pKey);
        }
    }

    @Rule
    public BundleContextClassLoaderRule rule = new BundleContextClassLoaderRule(this);

    @Inject
    private BundleContext context;
    private Bundle systemBundle;
    private final TestListener changeListener = new TestListener();
    private ServiceRegistration<FileChangeListener> hotdeployObserverRegistration;
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
                editConfigurationFilePut("etc/ch.sourcepond.io.hotdeployer.impl.Activator.cfg", "hotdeployDirectoryURI", TEST_DIR.toUri().toString()),
                karafContainer(features(hotdeployerRepo, "hotdeployer-feature"))
        };
    }

    private void writeArbitraryContent() throws Exception {
        try (final BufferedWriter writer = newBufferedWriter(testFile, CREATE, APPEND)) {
            writer.write(randomUUID().toString());
        }
        sleep(500);
    }

    @Before
    public void setup() throws Exception {
        createDirectories(TEST_DIR);
        systemBundle = context.getBundle(SYSTEM_BUNDLE_ID);
        hotdeployObserverRegistration = context.registerService(FileChangeListener.class, changeListener.mock, null);
    }

    @After
    public void tearDown() throws Exception {
        hotdeployObserverRegistration.unregister();
        walkFileTree(TEST_DIR, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                delete(file);
                return CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                if (!TEST_DIR.equals(dir)) {
                    delete(dir);
                }
                return CONTINUE;
            }
        });
    }

    private static boolean isKeyMatching(final DispatchKey pKey, final Path pRelativePath, final Bundle pSource) {
        return pRelativePath.equals(pKey.getRelativePath()) && pSource.equals(pKey.getDirectoryKey());
    }

    private static DispatchKey key(final Path pRelativePath, final Bundle pSource) {
        return argThat(new ArgumentMatcher<DispatchKey>() {
            @Override
            public boolean matches(final DispatchKey key) {
                return isKeyMatching(key, pRelativePath, pSource);
            }

            @Override
            public String toString() {
                return format("[%s:%s]", pSource, pRelativePath);
            }
        });
    }

    private static FileChangeEvent event(final Path pRelativePath, final Bundle pSource) {
        return argThat(new ArgumentMatcher<FileChangeEvent>() {
            @Override
            public boolean matches(final FileChangeEvent pEvent) {
                return isKeyMatching(pEvent.getKey(), pRelativePath, pSource);
            }

            @Override
            public String toString() {
                return format("[%s:%s]", pSource, pRelativePath);
            }
        });
    }


    @Test
    public void verifyKeyRelativeToBundlePath() throws Exception {
        final Bundle bundle = context.getBundle();
        final Path bundleRoot = createDirectories(TEST_DIR.resolve(
                format("%s%s", "$BUNDLE$_", bundle.getSymbolicName())).resolve(
                bundle.getVersion().toString()));
        testFile = bundleRoot.resolve("test.txt");

        writeArbitraryContent();

        // modified should have been called exactly once
        final Path relativePath = bundleRoot.relativize(testFile);
        verify(changeListener.mock, timeout(20000)).modified(event(relativePath, bundle));

        delete(testFile);
        verify(changeListener.mock, timeout(10000)).discard(key(relativePath, bundle));
    }

    @Test
    public void verifyModifyAndDiscard() throws Exception {
        testFile = TEST_DIR.resolve("test.txt");

        // Write every 500ms something into file; do this 10 times
        for (int i = 0; i < 10; i++) {
            writeArbitraryContent();
        }

        // modified should have been called exactly once
        final Path relativePath = TEST_DIR.relativize(testFile);
        verify(changeListener.mock, timeout(20000)).modified(event(relativePath, systemBundle));

        delete(testFile);
        verify(changeListener.mock, timeout(10000)).discard(key(relativePath, systemBundle));
    }
}
