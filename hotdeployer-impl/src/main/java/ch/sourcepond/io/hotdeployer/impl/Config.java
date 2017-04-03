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
            name="Naming patterns to be blacklisted",
            description = "Regular expressions to identify files/directories within the hotdeployment directory " +
                    "(see 'hotdeployDirectoryURI') which should be ignored i.e. not be handled by any observer."
    )
    String[] blacklistPatterns() default "";
}
