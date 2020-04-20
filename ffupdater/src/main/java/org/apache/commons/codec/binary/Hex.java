/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * git commit: bf561f5153183bef3946fdc4c9e68aae3dda260f
 */

package org.apache.commons.codec.binary;

import org.apache.commons.codec.DecoderException;

import java.nio.ByteBuffer;

/**
 * Converts hexadecimal Strings. The Charset used for certain operation can be set.
 *
 * This class is thread-safe.
 *
 * @since 1.1
 */
public class Hex {
    /**
     * Converts a String representing hexadecimal values into an array of bytes of those same values. The returned array
     * will be half the length of the passed String, as it takes two characters to represent any given byte. An
     * exception is thrown if the passed String has an odd number of elements.
     *
     * @param data A String containing hexadecimal digits
     * @return A byte array containing binary data decoded from the supplied char array.
     * @throws DecoderException Thrown if an odd number or illegal of characters is supplied
     * @since 1.11
     */
    public static byte[] decodeHex(final String data) throws DecoderException {
        return decodeHex(data.toCharArray());
    }

    /**
     * Converts an array of characters representing hexadecimal values into an array of bytes of those same values. The
     * returned array will be half the length of the passed array, as it takes two characters to represent any given
     * byte. An exception is thrown if the passed char array has an odd number of elements.
     *
     * @param data An array of characters containing hexadecimal digits
     * @return A byte array containing binary data decoded from the supplied char array.
     * @throws DecoderException Thrown if an odd number or illegal of characters is supplied
     */
    public static byte[] decodeHex(final char[] data) throws DecoderException {

        final int len = data.length;

        if ((len & 0x01) != 0) {
            throw new DecoderException("Odd number of characters.");
        }

        final byte[] out = new byte[len >> 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; j < len; i++) {
            int f = toDigit(data[j], j) << 4;
            j++;
            f = f | toDigit(data[j], j);
            j++;
            out[i] = (byte) (f & 0xFF);
        }

        return out;
    }

    /**
     * Converts a hexadecimal character to an integer.
     *
     * @param ch    A character to convert to an integer digit
     * @param index The index of the character in the source
     * @return An integer
     * @throws DecoderException Thrown if ch is an illegal hex character
     */
    protected static int toDigit(final char ch, final int index) throws DecoderException {
        final int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new DecoderException("Illegal hexadecimal character " + ch + " at index " + index);
        }
        return digit;
    }
}
