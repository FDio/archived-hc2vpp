package io.fd.honeycomb.translate.v3po.util;

import static io.fd.honeycomb.translate.v3po.util.TranslateUtils.reverseBytes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.JVppReply;

public class TranslateUtilsTest {

    private static class AnDataObject implements DataObject {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return null;
        }
    }

    @Test
    public void testGetReplyForWriteTimeout() throws Exception {
        final Future<JVppReply<?>> future = mock(Future.class);
        when(future.get(anyLong(), eq(TimeUnit.SECONDS))).thenThrow(TimeoutException.class);
        final InstanceIdentifier<AnDataObject> replyType = InstanceIdentifier.create(AnDataObject.class);
        try {
            TranslateUtils.getReplyForWrite(future, replyType);
        } catch (WriteTimeoutException e) {
            assertTrue(e.getCause() instanceof TimeoutException);
            assertEquals(replyType, e.getFailedId());
            return;
        }
        fail("WriteTimeoutException was expected");
    }

    @Test
    public void testGetReplyForReadTimeout() throws Exception {
        final Future<JVppReply<?>> future = mock(Future.class);
        final InstanceIdentifier<AnDataObject> replyType = InstanceIdentifier.create(AnDataObject.class);
        when(future.get(anyLong(), eq(TimeUnit.SECONDS))).thenThrow(TimeoutException.class);
        try {
            TranslateUtils.getReplyForRead(future, replyType);
        } catch (ReadTimeoutException e) {
            assertTrue(e.getCause() instanceof TimeoutException);
            assertEquals(replyType, e.getFailedId());
            return;
        }
        fail("ReadTimeoutException was expected");
    }

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