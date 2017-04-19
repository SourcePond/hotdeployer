/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.sourcepond.io.hotdeployer.impl;

import java.util.regex.PatternSyntaxException;

/**
 * A class for POSIX glob pattern with brace expansions. The code of this class is borrowed from
 * <a href="https://git-wip-us.apache.org/repos/asf?p=hadoop.git;a=blob;f=hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/GlobPattern.java;h=4be5b1cfb4db16e7a449df8d8963fd7129a9f3aa;hb=91f2b7a13d1e97be65db92ddabc627cc29ac0009">org.apache.hadoop.fs.GlobPattern</a>,
 * and, has been customized to meet the needs of this bundle.
 */
public class GlobPattern {
    static final char BACKSLASH = '\\';

    /**
     * Set and compile a glob pattern
     * @param pGlob  the glob pattern string
     */
    public static String convert(final String pGlob) {
        final StringBuilder regex = new StringBuilder();
        int setOpen = 0;
        int curlyOpen = 0;
        int len = pGlob.length();

        for (int i = 0; i < len; i++) {
            char c = pGlob.charAt(i);

            switch (c) {
                case BACKSLASH:
                    if (++i >= len) {
                        error("Missing escaped character", pGlob, i);
                    }
                    regex.append(c).append(pGlob.charAt(i));
                    continue;
                case '.':
                case '$':
                case '(':
                case ')':
                case '|':
                case '+':
                    // escape regex special chars that are not glob special chars
                    regex.append(BACKSLASH);
                    break;
                case '*':
                    regex.append('.');
                    break;
                case '?':
                    regex.append('.');
                    continue;
                case '{': // start of a group
                    regex.append("(?:"); // non-capturing
                    curlyOpen++;
                    continue;
                case ',':
                    regex.append(curlyOpen > 0 ? '|' : c);
                    continue;
                case '}':
                    if (curlyOpen > 0) {
                        // end of a group
                        curlyOpen--;
                        regex.append(")");
                        continue;
                    }
                    break;
                case '[':
                    if (setOpen > 0) {
                        error("Unclosed character class", pGlob, i);
                    }
                    setOpen++;
                    break;
                case '^': // ^ inside [...] can be unescaped
                    if (setOpen == 0) {
                        regex.append(BACKSLASH);
                    }
                    break;
                case '!': // [! needs to be translated to [^
                    regex.append(setOpen > 0 && '[' == pGlob.charAt(i - 1) ? '^' : '!');
                    continue;
                case ']':
                    // Many set errors like [][] could not be easily detected here,
                    // as []], []-] and [-] are all valid POSIX glob and java regex.
                    // We'll just let the regex compiler do the real work.
                    setOpen = 0;
                    break;
                default:
            }
            regex.append(c);
        }

        if (setOpen > 0) {
            error("Unclosed character class", pGlob, len);
        }
        if (curlyOpen > 0) {
            error("Unclosed group", pGlob, len);
        }
        return regex.toString();
    }

    private static void error(String message, String pattern, int pos) {
        throw new PatternSyntaxException(message, pattern, pos);
    }
}