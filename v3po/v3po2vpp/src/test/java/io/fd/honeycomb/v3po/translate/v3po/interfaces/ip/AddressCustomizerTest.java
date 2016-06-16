package io.fd.honeycomb.v3po.translate.v3po.interfaces.ip;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.MappingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.SwInterfaceAddDelAddress;
import org.openvpp.jvpp.dto.SwInterfaceAddDelAddressReply;
import org.openvpp.jvpp.future.FutureJVpp;

public class AddressCustomizerTest {

    @Test
    public void testWriteCurrentAttributes() throws WriteFailedException {

        ArgumentCaptor<SwInterfaceAddDelAddress> requestCaptor = ArgumentCaptor.forClass(SwInterfaceAddDelAddress.class);

        FutureJVpp jvpp = mock(FutureJVpp.class);
        WriteContext context = mock(WriteContext.class);
        MappingContext mappingContext = mock(MappingContext.class);
        NamingContext namingContext = new NamingContext("prefix", "instance");

        namingContext.addName(5, "parent", mappingContext);

        InterfaceKey key = new InterfaceKey("local0");

        InstanceIdentifier<Address> id = InstanceIdentifier.builder(Interfaces.class)
                .child(Interface.class,key)
                .augmentation(Interface1.class)
                .child(Ipv4.class)
                .child(Address.class)
                .build();

        Mapping mapping = mock(Mapping.class);

        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));

        PrefixLength length = new PrefixLengthBuilder().setPrefixLength(new Integer(24).shortValue()).build();
        Address data = new AddressBuilder().setIp(noZoneIp).setSubnet(length).build();

        CompletableFuture<SwInterfaceAddDelAddressReply> future = new CompletableFuture<>();
        future.complete(new SwInterfaceAddDelAddressReply());

        when(context.getMappingContext()).thenReturn(mappingContext);
        when(mapping.getIndex()).thenReturn(5);
        when(mapping.getName()).thenReturn("local0");
        when(mappingContext.read(Mockito.any())).thenReturn(Optional.fromNullable(mapping));
        when(jvpp.swInterfaceAddDelAddress(Mockito.any(SwInterfaceAddDelAddress.class))).thenReturn(future);

        new AddressCustomizer(jvpp, namingContext).writeCurrentAttributes(id, data, context);

        verify(jvpp, times(1)).swInterfaceAddDelAddress(requestCaptor.capture());

        SwInterfaceAddDelAddress request = requestCaptor.getValue();

        assertEquals(0, request.isIpv6);
        assertEquals(1, request.isAdd);
        assertEquals(0, request.delAll);
        assertEquals(5, request.swIfIndex);
        assertEquals(24, request.addressLength);
        assertEquals(true,Arrays.equals(new byte[]{-64, -88, 2, 1}, request.address));
    }

    @Test
    public void testDeleteCurrentAttributes() throws WriteFailedException {
        ArgumentCaptor<SwInterfaceAddDelAddress> requestCaptor = ArgumentCaptor.forClass(SwInterfaceAddDelAddress.class);

        FutureJVpp jvpp = mock(FutureJVpp.class);
        WriteContext context = mock(WriteContext.class);
        MappingContext mappingContext = mock(MappingContext.class);
        NamingContext namingContext = new NamingContext("prefix", "instance");

        namingContext.addName(5, "parent", mappingContext);

        InterfaceKey key = new InterfaceKey("local0");

        InstanceIdentifier<Address> id = InstanceIdentifier.builder(Interfaces.class)
                .child(Interface.class,key)
                .augmentation(Interface1.class)
                .child(Ipv4.class)
                .child(Address.class)
                .build();

        Mapping mapping = mock(Mapping.class);

        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));

        PrefixLength length = new PrefixLengthBuilder().setPrefixLength(new Integer(24).shortValue()).build();
        Address data = new AddressBuilder().setIp(noZoneIp).setSubnet(length).build();

        CompletableFuture<SwInterfaceAddDelAddressReply> future = new CompletableFuture<>();
        future.complete(new SwInterfaceAddDelAddressReply());

        when(context.getMappingContext()).thenReturn(mappingContext);
        when(mapping.getIndex()).thenReturn(5);
        when(mapping.getName()).thenReturn("local0");
        when(mappingContext.read(Mockito.any())).thenReturn(Optional.fromNullable(mapping));
        when(jvpp.swInterfaceAddDelAddress(Mockito.any(SwInterfaceAddDelAddress.class))).thenReturn(future);

        new AddressCustomizer(jvpp, namingContext).deleteCurrentAttributes(id, data, context);

        verify(jvpp, times(1)).swInterfaceAddDelAddress(requestCaptor.capture());

        SwInterfaceAddDelAddress request = requestCaptor.getValue();

        assertEquals(0, request.isIpv6);
        assertEquals(0, request.isAdd);
        assertEquals(0, request.delAll);
        assertEquals(5, request.swIfIndex);
        assertEquals(24, request.addressLength);
        assertEquals(true,Arrays.equals(new byte[]{-64, -88, 2, 1}, request.address));
    }

    @Test
    public void testExtract() {
        Address address = new AddressBuilder().build();
        Ipv4 parentData = new Ipv4Builder().setAddress(Arrays.asList(address)).build();

        Optional<List<Address>> addressesOptional = new AddressCustomizer(mock(FutureJVpp.class),null).extract(null, parentData);

        assertEquals(true,addressesOptional.isPresent());
        assertEquals(1,addressesOptional.get().size());
        assertEquals(true,addressesOptional.get().contains(address));
    }

}
