package io.fd.honeycomb.v3po.translate.v3po.utils;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class V3poUtilsTest {

    @Test
    public void testToString() {
        final byte[] expected = "test".getBytes();
        final byte[] cString = new byte[expected.length+10];
        System.arraycopy(expected, 0, cString, 0, expected.length);
        final String jString = V3poUtils.toString(cString);
        assertArrayEquals(expected, jString.getBytes());
    }
}