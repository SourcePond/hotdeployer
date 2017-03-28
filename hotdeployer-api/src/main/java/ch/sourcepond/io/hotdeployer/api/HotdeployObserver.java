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
package ch.sourcepond.io.hotdeployer.api;

import java.io.IOException;
import java.nio.file.Path;

/**
 * <p>Observer to receive notifications about changes on files
 * within a watched directory and its sub-directories.</p>
 * <p><em>Implementations of this interface must be thread-safe.</em></p>
 */
public interface HotdeployObserver {

    /**
     * <p>
     * Indicates, that the file specified has been modified. Modified means,
     * that the file has been created or updated. This method takes two parameters:
     * <h3>Relative path</h3>
     * This path is relative to the watched directory. This path <em>cannot</em> be used to read any data.
     * The relative path always remains the same for a specific file, even when the underlying
     * watched directory (and therefore the absolute file) has been updated to point to another location.
     * Because this, use the relative path for any caching of objects created out of the file data.
     * <h3>Readable Path</h3>
     * This is the (absolute) path which can be opened for reading. The readable path of a file can change in
     * case when the underlying watched directory (and therefore the absolute file) is updated to point to another
     * location. Because this, do <em>not</em> use the readable path for any caching, but, only for reading (or writing)
     * data.
     * <p>Following code snipped should give an idea how caching of an object created out of the readable path
     * should be implemented:
     * <pre>
     *      final Map&lt;FileKey, Object&gt; cache = ...
     *      cache.put(pKey, readObject(pFile));
     * </pre>
     *
     * @param pRelativePath Relative-path of the modified file, never {@code null}
     * @param pFile         Readable path, never {@code null}
     * @throws IOException Thrown, if the modified path could not be read.
     */
    void modified(Path pRelativePath, Path pFile) throws IOException;

    /**
     * <p>Indicates, that the file or directory with the relative path specified has been discarded for some reason
     * (file/directory has been deleted, watched directory is being unregistered etc.). Depending on the operating
     * system, the delivered keys can <em>differ in case when a directory has been deleted recursively</em>. For instance, on
     * systems with a native {@link java.nio.file.WatchService} implementation you will probably get a relative path
     * for every deleted file and directory. On other systems which work with the default polling watch-service you
     * likely only get the file key of the deleted base directory.</p>
     * <p>If you work with cached objects and you want to avoid different behaviour on varying operating systems,
     * resource discarding can be safely implemented as follows:
     * <pre>
     *      final Map&lt;Path, Object&gt; cache = ...
     *
     *      // Remove any key which is a sub-key of pRelativePath.
     *      cache.keySet().removeIf(k -&gt; k.startsWith(pRelativePath));
     * </pre>
     *
     * @param pRelativePath Relative path of the discarded file or directory, never {@code null}
     */
    void discard(Path pRelativePath);
}
