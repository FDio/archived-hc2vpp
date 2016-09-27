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