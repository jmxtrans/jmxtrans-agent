/*
 * Copyright (c) 2010-2013 the original author or authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.jmxtrans.embedded.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Yet another {@code StringUtils} class.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class StringUtils2 {

    private StringUtils2() {

    }

    /**
     * Split given String. Delimiters are <code>","</code>, <code>";"</code> and <code>"\n"</code>.
     *
     * @param delimitedString
     * @return splitted string or <code>null</code> if given <code>delimitedString</code> is <code>null</code>
     */
    public static List<String> delimitedStringToList(@Nullable String delimitedString) {
        if (delimitedString == null) {
            return null;
        }
        String[] splits = delimitedString.split("[,;\\n]");

        List<String> result = new ArrayList<String>();

        for (String split : splits) {
            split = split.trim();
            if (!split.isEmpty()) {
                result.add(split);
            }
        }
        return result;
    }

    /**
     * Join given {@code tokens} with given {@code delimiter}.
     * <p/>
     * Sample: tokens <code>"com", "mycompany, "ecommerce", "server1"</code> with delimiter <code>"."</code>
     * returns <code>"com.mycompany.ecommerce.server1"</code>.
     *
     * @param tokens
     * @param delimiter
     * @return the joined tokens (<code>null</code> if given <code>tokens</code> is <code>null</code>
     */
    public static String join(@Nullable List<String> tokens, @Nonnull String delimiter) {
        if (tokens == null) {
            return null;
        }
        Preconditions.checkNotNull(delimiter, "given delimiter can not be null");

        Iterator<String> it = tokens.iterator();
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            String token = it.next();
            sb.append(token);
            if (it.hasNext()) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    /**
     * <p> Reverse tokens of given tokenized <code>str</code>.</p>
     * <p>Sample: "server1.ecommerce.mycompany.com" returns <code>"com.mycompany.ecommerce.server1"</code>.</p>
     *
     * @param str
     * @param delimiter
     * @return reversed string or <code>null</code> if given string is <code>null</code>
     */
    public static String reverseTokens(@Nullable String str, @Nonnull String delimiter) {
        if (str == null) {
            return null;
        }
        Preconditions.checkNotNull(delimiter, "given delimiter can not be null");

        StringTokenizer st = new StringTokenizer(str, delimiter);
        List<String> tokens = new ArrayList<String>();

        while (st.hasMoreTokens()) {
            tokens.add(st.nextToken());
        }
        Collections.reverse(tokens);

        return join(tokens, delimiter);
    }
}
