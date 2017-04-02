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

import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static ch.sourcepond.io.fileobserver.spi.WatchedDirectory.create;
import static java.lang.System.getProperty;
import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Paths.get;

/**
 *
 */
class DirectoryFactory {
    static final Path DEFAULT_HOTDEPLOY_DIRECTORY = getDefault().getPath(getProperty("java.io.tmpdir"), "sourcepond", "hotdeploy");
    static final String DIRECTORY_KEY = "hotdeploymentDirectory";

    Path getHotdeployDir(final Config pConfig) throws IOException, URISyntaxException {
        final Path hotdeployDir;
        if (pConfig.hotdeployDirectoryURI().isEmpty()) {
            hotdeployDir = DEFAULT_HOTDEPLOY_DIRECTORY;
        } else {
            hotdeployDir = get(new URI(pConfig.hotdeployDirectoryURI()));
        }
        return createDirectories(hotdeployDir);
    }

    WatchedDirectory newWatchedDirectory(final Config pConfig) throws IOException, URISyntaxException {
        final WatchedDirectory directory = create(DIRECTORY_KEY, getHotdeployDir(pConfig));
        for (final String pattern : pConfig.blacklistPatterns()) {
            directory.addBlacklistPattern(pattern);
        }
        return directory;
    }
}
