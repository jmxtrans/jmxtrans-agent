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
package org.jmxtrans.agent.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Yet another {@code StringUtils} class.
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class StringUtils2 {

    private StringUtils2() {

    }

    /**
     * Split given String. Delimiters are {@code ","}, {@code ";"} and {@code "\n"}.
     *
     * @param delimitedString
     * @return splitted string or {@code null} if given {@code delimitedString} is {@code null}
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
     *
     * Sample: tokens {@code "com", "mycompany, "ecommerce", "server1"} with delimiter {@code "."}
     * returns {@code "com.mycompany.ecommerce.server1"}.
     *
     * @param tokens
     * @param delimiter
     * @return the joined tokens ({@code null} if given {@code tokens} is {@code null}
     */
    public static String join(@Nullable List<String> tokens, @Nonnull String delimiter) {
        if (tokens == null) {
            return null;
        }
        Preconditions2.checkNotNull(delimiter, "given delimiter can not be null");

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
     * <p> Reverse tokens of given tokenized {@code str}.</p>
     * <p>Sample: "server1.ecommerce.mycompany.com" returns {@code "com.mycompany.ecommerce.server1"}.</p>
     *
     * @param str
     * @param delimiter
     * @return reversed string or {@code null} if given string is {@code null}
     */
    public static String reverseTokens(@Nullable String str, @Nonnull String delimiter) {
        if (str == null) {
            return null;
        }
        Preconditions2.checkNotNull(delimiter, "given delimiter can not be null");

        StringTokenizer st = new StringTokenizer(str, delimiter);
        List<String> tokens = new ArrayList<String>();

        while (st.hasMoreTokens()) {
            tokens.add(st.nextToken());
        }
        Collections.reverse(tokens);

        return join(tokens, delimiter);
    }

    /**
     * Escape all non 'a' to 'z','A' to 'Z', '0' to '9' and '-' with a '_'.
     *
     * '.' is escaped with a '_' if {@code escapeDot} is {@code true}.
     *
     * @param str       the string to escape
     * @param escapeDot indicates whether '.' should be escaped into '_' or not.
     * @param result    the {@linkplain StringBuilder} in which the escaped string is appended
     */
    public static void appendEscapedNonAlphaNumericChars(@Nonnull String str, boolean escapeDot, @Nonnull StringBuilder result) {
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (Character.isLetter(ch) || Character.isDigit(ch) || ch == '-') {
                result.append(ch);
            } else if (ch == '.') {
                result.append(escapeDot ? '_' : ch);
            } else if (ch == '"' && ((i == 0) || (i == chars.length - 1))) {
                // ignore starting and ending '"' that are used to quote() objectname's values (see ObjectName.value())
            } else {
                result.append('_');
            }
        }
    }

    /**
     * Escape all non 'a' to 'z','A' to 'Z', '0' to '9' and '-' with a '_'.
     *
     * @param str    the string to escape
     * @param result the {@linkplain StringBuilder} in which the escaped string is appended
     */
    public static void appendEscapedNonAlphaNumericChars(@Nonnull String str, @Nonnull StringBuilder result) {
        appendEscapedNonAlphaNumericChars(str, true, result);
    }

    public static boolean isNullOrEmpty(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Abbreviates a String using ellipses. This will turn "Now is the time for all good men" into "Now is the time for..."
     *
     * @param str the String to abbreviate
     * @param max the max number of chars of the abbreviated String
     * @return the abbreviated string
     */
    @Nonnull
    public static String abbreviate(String str, int max) {
        if (str == null) {
            return "";
        } else if (str.length() <= max) {
            return str;
        } else {
            return str.substring(0, max - 3) + "...";
        }
    }
}
