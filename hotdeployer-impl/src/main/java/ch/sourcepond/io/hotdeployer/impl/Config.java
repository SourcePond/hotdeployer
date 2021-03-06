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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *
 */
@ObjectClassDefinition(name = "Sourcepond hotdeployer configuration")
public @interface Config {

    @AttributeDefinition(
            name = "Hotdeployment directory URI",
            description = "URI which points to the watched root directory, for example file:///tmp/hotdeploy")
    String hotdeployDirectoryURI() default "";

    @AttributeDefinition(
            name = "Bundle resource directory prefix",
            description = "If resources are bound to a specific bundle, they must be placed within special " +
                    "sub-directories of the hotdeployment directory (see 'hotdeployDirectoryURI'). These sub-directories " +
                    "must be specified as follows: A) the first sub-directory must meet naming pattern " +
                    "${bundleResourceDirectoryPrefix}Bundle-SymbolicName and needs to be placed directly within " +
                    "the hotdeployment directory (example: __BUNDLE__com.foo.bar). B) The second sub-directory must be " +
                    "placed within the previously mentioned bundle-directory, and, must be named like the version of " +
                    "the desired bundle (example: 1.0.0.20170101_1234). This property specifies the prefix which is" +
                    "necessary to decide whether a directory is such a special directory or not.")
    String bundleResourceDirectoryPrefix() default "__BUNDLE__";

    @AttributeDefinition(
            name="Naming patterns to be blacklisted",
            description = "Expressions supported by the filesystem (like glob, see https://docs.oracle.com/javase/8/docs" +
                    "/api/java/nio/file/FileSystem.html#getPathMatcher-java.lang.String-) to identify files/directories " +
                    "within the hotdeployment directory (see 'hotdeployDirectoryURI') which should be ignored i.e. not be handled by any listener."
    )
    String blacklistPatterns() default "";

    @AttributeDefinition(
            name="TimeUnit of the bundle availability timeout",
            description = "Specifies which time-unit is used for property 'bundleAvailabilityTimeout'."
    )
    TimeUnit bundleAvailabilityTimeoutUnit() default SECONDS;

    @AttributeDefinition(
            name="Bundle availability timeout",
            description = "Specifies after how much time a postponed change event shall be omitted. After the " +
                    "time elapses, the causing exception of the delay is logged and the event will be dropped."
    )
    long bundleAvailabilityTimeout() default 240L;
}
