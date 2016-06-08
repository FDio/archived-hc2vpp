package io.fd.honeycomb.v3po.translate.v3po.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;

public class TranslateUtilsTest {

    @Test
    public void testIpv4NoZone() throws Exception {
        final Ipv4AddressNoZone ipv4Addr = new Ipv4AddressNoZone("192.168.1.1");
        byte[] bytes = TranslateUtils.ipv4AddressNoZoneToArray(ipv4Addr);
        assertEquals((byte)192, bytes[0]);
        // Simulating the magic of VPP
        bytes = reverseBytes(bytes);
        final Ipv4AddressNoZone ipv4AddressNoZone = TranslateUtils.arrayToIpv4AddressNoZone(bytes);
        assertEquals(ipv4Addr, ipv4AddressNoZone);
    }

    private byte[] reverseBytes(final byte[] bytes) {
        final byte[] reversed = new byte[bytes.length];
        int i = 1;
        for (byte aByte : bytes) {
            reversed[bytes.length - i++] = aByte;
        }

        return reversed;
    }

    @Test
    public void testToString() {
        final byte[] expected = "test".getBytes();
        final byte[] cString = new byte[expected.length + 10];
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
        assertEquals((byte) 0, bytes[0]);
        assertEquals((byte) 255, bytes[1]);
        assertEquals((byte) 127, bytes[2]);
        assertEquals((byte) 21, bytes[3]);
        assertEquals((byte) 94, bytes[4]);
        assertEquals((byte) 169, bytes[5]);
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

    @Test
    public void testBooleanToByte() {
        assertEquals(0, TranslateUtils.booleanToByte(null));
        assertEquals(0, TranslateUtils.booleanToByte(false));
        assertEquals(1, TranslateUtils.booleanToByte(true));
    }

    @Test
    public void testByteToBoolean() {
        assertEquals(Boolean.FALSE, TranslateUtils.byteToBoolean((byte) 0));
        assertEquals(Boolean.TRUE, TranslateUtils.byteToBoolean((byte) 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testByteToBooleanFailed() {
        TranslateUtils.byteToBoolean((byte) 123);
    }
}