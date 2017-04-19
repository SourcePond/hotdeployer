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

import ch.sourcepond.io.fileobserver.api.SimpleDispatchRestriction;

import static ch.sourcepond.io.hotdeployer.impl.GlobPattern.BACKSLASH;
import static java.lang.String.valueOf;

/**
 *
 */
class DispatchRestrictionProxy implements SimpleDispatchRestriction {

    @FunctionalInterface
    private static interface Converter {

        String convert(String pPattern);
    }

    private static final String VERSION_RANGE_PATTERN = "^(\\d+(\\.\\d+(\\.\\d+(\\.[\\d\\w-]+)?)?)?)$|^" +
            "([\\[\\(]\\d+(\\.\\d+(\\.\\d+(\\.[\\d\\w-]+)?)?)?(,\\s*)?(\\d+(\\.\\d+(\\.\\d+(\\.[\\d\\w-]+)?)?)?)?" +
            "(\\]|\\)))$";
    private final SimpleDispatchRestriction delegate;
    private final String bundlePrefixPattern;

    DispatchRestrictionProxy(final SimpleDispatchRestriction pDelegate,
                             final String pPrefix) {
        delegate = pDelegate;
        bundlePrefixPattern = "^" + escape(pPrefix, "\\^$.|?*+()[{") + ".*";
    }

    private static String escape(final String pPrefix, final String pToBeEscaped) {
        String result = pPrefix;
        for (final char toBeEscaped : pToBeEscaped.toCharArray()) {
            final String replacementPattern = valueOf(BACKSLASH) + toBeEscaped;
            result = result.replaceAll(replacementPattern, valueOf(toBeEscaped));
        }
        return result;
    }

    private String[] buildRegex(final Converter pConverter, final String... pPatterns) {
        final String[] combined = new String[pPatterns.length + 2];
        combined[0] = bundlePrefixPattern;
        combined[1] = VERSION_RANGE_PATTERN;

        for (int i = 0, z = 2; i < pPatterns.length; i++, z++) {
            combined[z] = pConverter.convert(pPatterns[i]);
        }

        return combined;
    }

    @Override
    public SimpleDispatchRestriction addGlob(final String... pPatterns) {
        delegate.addGlob(pPatterns);
        delegate.addRegex(buildRegex(GlobPattern::convert, pPatterns));
        return this;
    }

    @Override
    public SimpleDispatchRestriction addRegex(final String... pPatterns) {
        delegate.addRegex(pPatterns);
        delegate.addRegex(buildRegex(s -> s, pPatterns));
        return this;
    }
}
