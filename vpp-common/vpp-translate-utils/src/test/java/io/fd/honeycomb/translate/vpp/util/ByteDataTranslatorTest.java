/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.translate.vpp.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Created by jsrnicek on 23.9.2016.
 */
public class ByteDataTranslatorTest implements ByteDataTranslator {

    @Test
    public void testBooleanToByte() {
        assertEquals(0, booleanToByte(null));
        assertEquals(0, booleanToByte(false));
        assertEquals(1, booleanToByte(true));
    }

    @Test
    public void testByteToBoolean() {
        assertEquals(Boolean.FALSE, byteToBoolean((byte) 0));
        assertEquals(Boolean.TRUE, byteToBoolean((byte) 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testByteToBooleanFailed() {
        byteToBoolean((byte) 123);
    }

    @Test
    public void testToString() {
        final byte[] expected = "test".getBytes();
        final byte[] cString = new byte[expected.length + 10];
        System.arraycopy(expected, 0, cString, 0, expected.length);
        final String jString = toString(cString);
        assertArrayEquals(expected, jString.getBytes());
    }
}