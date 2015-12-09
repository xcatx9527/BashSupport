/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ansorgit.plugins.bash.lang.parser.eval;

import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

/**
 * Handles a string which has just the escape codes
 * The Bash manual says about escape codes in strings:
 * <pre>
 *      Enclosing characters in double quotes preserves the literal value of all characters within the quotes, with the
 * exception of $, `, \, and, when history expansion is enabled, !.  The characters $ and ` retain  their  special
 * meaning  within double quotes.  The backslash retains its special meaning only when followed by one of the fol-
 * lowing characters: $, `, ", \, or <newline>.  A double quote may be quoted within double quotes by preceding it
 * with  a  backslash.  If enabled, history expansion will be performed unless an !  appearing in double quotes is
 * escaped using a backslash.  The backslash preceding the !  is not removed.
 *  </pre>
 *
 * @author jansorg
 */
@SuppressWarnings("Duplicates")
public class BashEnhancedTextPreprocessor implements TextPreprocessor {
    private int[] outSourceOffsets;
    private TextRange contentRange;

    public BashEnhancedTextPreprocessor(TextRange contentRange) {
        this.contentRange = contentRange;
    }

    /**
     * Handles escape codes in evaluated string, e.g. the string in
     * <code>eval "echo \˜This is the value of \$x: $x\""</code>
     *
     * @param chars
     * @param outChars
     * @param sourceOffsetsRef
     * @return
     */
    private static boolean parseStringCharacters(String chars, StringBuilder outChars, Ref<int[]> sourceOffsetsRef) {
        int[] sourceOffsets = new int[chars.length() + 1];
        sourceOffsetsRef.set(sourceOffsets);

        //init with -1
        TextProcessorUtil.resetOffsets(sourceOffsets);

        if (chars.indexOf('\\') < 0) {
            outChars.append(chars);
            for (int i = 0; i < sourceOffsets.length; i++) {
                sourceOffsets[i] = i;
            }
            return true;
        }

        int index = 0;
        while (index < chars.length()) {
            char c = chars.charAt(index++);

            sourceOffsets[outChars.length()] = index - 1;
            sourceOffsets[outChars.length() + 1] = index;

            if (c != '\\') {
                outChars.append(c);
                continue;
            }

            if (index == chars.length()) {
                return false;
            }

            c = chars.charAt(index++);
            switch (c) {
                //newline
                case 'n':
                    outChars.append('\n');
                    break;

                //return
                case 'r':
                    outChars.append('\r');
                    break;

                //tab
                case 't':
                    outChars.append('\t');
                    break;

                //vertical tab
                case 'v':
                    outChars.append(0x0B);
                    break;

                //backspace
                case 'b':
                    outChars.append('\b');
                    break;

                //alert
                case 'a':
                    outChars.append(0x07);
                    break;

                //escaped dollar
                case '$':
                case '"':
                case '\'':
                case '\\':
                    outChars.append(c);
                    break;

                //octal
                case '0':
                    //fixme handle 1 to 3 possible octal numbers
                    if (index + 2 <= chars.length()) {
                        try {
                            int v = Integer.parseInt(chars.substring(index, index + 2), 8);
                            outChars.append((char) v);
                            index += 2;
                        } catch (Exception e) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                    break;

                //all other escape codes do not change the content
                default:
                    outChars.append('\\');
                    outChars.append(c);
                    break;
            }

            sourceOffsets[outChars.length()] = index;
        }
        return true;
    }

    @Override
    public boolean decode(String content, @NotNull StringBuilder outChars) {
        Ref<int[]> sourceOffsetsRef = new Ref<int[]>();

        boolean result = parseStringCharacters(content, outChars, sourceOffsetsRef);

        this.outSourceOffsets = sourceOffsetsRef.get();

        return result;
    }

    public int getOffsetInHost(int offsetInDecoded) {
        int result = offsetInDecoded >= 0 && offsetInDecoded < outSourceOffsets.length ? outSourceOffsets[offsetInDecoded] : -1;
        if (result == -1) {
            return -1;
        }

        return contentRange.getStartOffset() + (result <= contentRange.getLength() ? result : contentRange.getLength());
    }

    @Override
    public TextRange getContentRange() {
        return contentRange;
    }

    @Override
    public boolean containsRange(int tokenStart, int tokenEnd) {
        return getContentRange().containsRange(tokenStart, tokenEnd);
    }
}