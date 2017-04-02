package ch.sourcepond.io.hotdeployer.impl;

import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Path;

import static ch.sourcepond.io.hotdeployer.impl.DirectoryFactory.DEFAULT_HOTDEPLOY_DIRECTORY;
import static ch.sourcepond.io.hotdeployer.impl.DirectoryFactory.DIRECTORY_KEY;
import static java.lang.System.getProperty;
import static java.nio.file.FileSystems.getDefault;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DirectoryFactoryTest {
    private static final String JAVA_IO_TMPDIR_PROPERTY = "java.io.tmpdir";
    private static final String ANY_PATTERN = "anyPattern";
    static final Path TEST_DIR = getDefault().getPath(getProperty("user.dir"), "target");
    private static String javaIoTmpdir;
    private final Path blacklistedPath = mock(Path.class, withSettings().name(ANY_PATTERN));
    private final DirectoryFactory factory = new DirectoryFactory();
    private final Config config = mock(Config.class);

    @BeforeClass
    public static void setJavaIoTmpdir() {
        javaIoTmpdir = System.getProperty(JAVA_IO_TMPDIR_PROPERTY);
        System.setProperty(JAVA_IO_TMPDIR_PROPERTY, TEST_DIR.toString());
    }

    @AfterClass
    public static void resetJavaIoTmpdir() {
        System.setProperty(JAVA_IO_TMPDIR_PROPERTY, javaIoTmpdir);
    }

    @Before
    public void setup() {
        when(config.blacklistPatterns()).thenReturn(new String[] {ANY_PATTERN});
    }

    @Test
    public void customHotdeploymentDirectoryConfigured() throws Exception {
        when(config.hotdeployDirectoryURI()).thenReturn(TEST_DIR.toUri().toString());
        final WatchedDirectory dir = factory.newWatchedDirectory(config);
        assertNotNull(dir);
        assertEquals(DIRECTORY_KEY, dir.getKey());
        assertEquals(TEST_DIR, dir.getDirectory());
        assertTrue(dir.isBlacklisted(blacklistedPath));
    }

    @Test
    public void useDefaultDirectory() throws Exception {
        when(config.hotdeployDirectoryURI()).thenReturn("");
        final WatchedDirectory dir = factory.newWatchedDirectory(config);
        assertNotNull(dir);
        assertEquals(DIRECTORY_KEY, dir.getKey());
        assertEquals(DEFAULT_HOTDEPLOY_DIRECTORY, dir.getDirectory());
        assertTrue(dir.isBlacklisted(blacklistedPath));
    }
}
