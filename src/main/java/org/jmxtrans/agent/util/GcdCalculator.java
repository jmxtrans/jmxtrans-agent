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

import java.math.BigInteger;
import java.util.List;

/**
 * @author Kristoffer Erlandsson
 */
public class GcdCalculator {

    /**
     * Finds the greatest common divisor of all numbers in the list.
     * 
     * @return the GCD or Long.MAX_VALUE if the list is empty.
     */
    public static long gcd(List<Long> l) {
        if (l.isEmpty()) {
            throw new IllegalArgumentException("List must contain at least one element");
        }
        BigInteger gcd = BigInteger.valueOf(l.get(0));
        for (Long num : l.subList(1, l.size())) {
            gcd = gcd.gcd(BigInteger.valueOf(num));
        }
        return gcd.longValue();
        
    }

}
