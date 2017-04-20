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

import ch.sourcepond.io.fileobserver.api.PathMatcherBuilder;
import ch.sourcepond.io.fileobserver.api.SimpleDispatchRestriction;

import java.nio.file.PathMatcher;

/**
 *
 */
class DispatchRestrictionProxy implements SimpleDispatchRestriction {
    static final String VERSION_RANGE_PATTERN = "^(\\d+(\\.\\d+(\\.\\d+(\\.[\\d\\w-]+)?)?)?)$|^" +
            "([\\[\\(]\\d+(\\.\\d+(\\.\\d+(\\.[\\d\\w-]+)?)?)?(,\\s*)?(\\d+(\\.\\d+(\\.\\d+(\\.[\\d\\w-]+)?)?)?)?" +
            "(\\]|\\)))$";
    private final SimpleDispatchRestriction delegate;
    private final String bundlePrefixPattern;

    DispatchRestrictionProxy(final String pPrefix, final SimpleDispatchRestriction pDelegate) {
        delegate = pDelegate;
        bundlePrefixPattern = "^" + escape(pPrefix, "\\^$.|?*+()[{") + ".*";
    }

    private static String escape(final String pPrefix, final String pToBeEscaped) {
        String result = pPrefix;
        final StringBuilder builder = new StringBuilder(pPrefix);
        for (final char toBeEscaped : pToBeEscaped.toCharArray()) {
            for (int i = 0 ; i < builder.length() ; i++) {
                if (builder.charAt(i) == toBeEscaped) {
                    builder.insert(i++, '\\');
                }
            }
        }
        return result;
    }

    @Override
    public PathMatcherBuilder whenPathMatchesPattern(final String pSyntax, final String pPattern) {
        delegate.whenPathMatchesRegex(bundlePrefixPattern).andRegex(VERSION_RANGE_PATTERN).andPattern(pSyntax, pPattern);
        return delegate.whenPathMatchesPattern(pSyntax, pPattern);
    }

    @Override
    public PathMatcherBuilder whenPathMatches(final PathMatcher pMatcher) {
        return delegate.whenPathMatches(pMatcher);
    }
}
