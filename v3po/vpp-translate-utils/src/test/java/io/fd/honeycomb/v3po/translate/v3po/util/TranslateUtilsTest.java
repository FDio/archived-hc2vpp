package io.fd.honeycomb.v3po.translate.v3po.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import org.junit.Test;

public class TranslateUtilsTest {

    @Test
    public void testToString() {
        final byte[] expected = "test".getBytes();
        final byte[] cString = new byte[expected.length+10];
        System.arraycopy(expected, 0, cString, 0, expected.length);
        final String jString = TranslateUtils.toString(cString);
        assertArrayEquals(expected, jString.getBytes());
    }

    @Test
    public void testParseMac() throws Exception {
        byte[] bytes = TranslateUtils.parseMac("00:fF:7f:15:5e:A9");
        assertMac(bytes);
    }

    private void assertMac(final byte[] bytes) {
        assertEquals(6, bytes.length);
        assertEquals((byte)0, bytes[0]);
        assertEquals((byte)255, bytes[1]);
        assertEquals((byte)127, bytes[2]);
        assertEquals((byte)21, bytes[3]);
        assertEquals((byte)94, bytes[4]);
        assertEquals((byte)169, bytes[5]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseMacLonger() throws Exception {
        byte[] bytes = TranslateUtils.parseMac("00:fF:7f:15:5e:A9:88:77");
        assertMac(bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseMacShorter() throws Exception {
        TranslateUtils.parseMac("00:fF:7f");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseRandomString() throws Exception {
        TranslateUtils.parseMac("random{}}@$*&*!");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseMacNumberFormatEx() throws Exception {
        TranslateUtils.parseMac("00:XX:7f:15:5e:77\"");
    }

   public void testBooleanToByte() {
       assertEquals(0, TranslateUtils.booleanToByte(null));
       assertEquals(0, TranslateUtils.booleanToByte(false));
       assertEquals(1, TranslateUtils.booleanToByte(true));
   }
}