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

package io.fd.honeycomb.v3po.translate.v3po.interfaces.ip;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.Subnet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.Netmask;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.dto.SwInterfaceAddDelAddress;
import org.openvpp.jvpp.dto.SwInterfaceAddDelAddressReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for writing {@link Address}
 */
public class AddressCustomizer extends FutureJVppCustomizer implements ListWriterCustomizer<Address, AddressKey> {

    private static final Logger LOG = LoggerFactory.getLogger(AddressCustomizer.class);
    private final NamingContext interfaceContext;

    public AddressCustomizer(FutureJVpp futureJvpp, NamingContext interfaceContext) {
        super(futureJvpp);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void writeCurrentAttributes(InstanceIdentifier<Address> id, Address dataAfter, WriteContext writeContext)
        throws WriteFailedException {
        setAddress(true, id, dataAfter, writeContext);
    }

    @Override
    public void updateCurrentAttributes(InstanceIdentifier<Address> id, Address dataBefore, Address dataAfter,
                                        WriteContext writeContext) throws WriteFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
            new UnsupportedOperationException("Operation not supported"));
    }

    @Override
    public void deleteCurrentAttributes(InstanceIdentifier<Address> id, Address dataBefore, WriteContext writeContext)
        throws WriteFailedException {
        setAddress(false, id, dataBefore, writeContext);
    }

    @Override
    public Optional<List<Address>> extract(InstanceIdentifier<Address> currentId, DataObject parentData) {
        return Optional.fromNullable((((Ipv4) parentData).getAddress()));
    }

    private void setAddress(boolean add, final InstanceIdentifier<Address> id, final Address address,
                            final WriteContext writeContext) throws WriteFailedException {

        final String interfaceName = id.firstKeyOf(Interface.class).getName();
        final int swIfc = interfaceContext.getIndex(interfaceName, writeContext.getMappingContext());

        Subnet subnet = address.getSubnet();

        if (subnet instanceof PrefixLength) {
            setPrefixLengthSubnet(add, id, interfaceName, swIfc, address, (PrefixLength) subnet);
        } else if (subnet instanceof Netmask) {
            setNetmaskSubnet(add, id, interfaceName, swIfc, address, (Netmask) subnet);
        } else {
            // FIXME how does choice extensibility work
            // FIXME it is not even possible to create a dedicated
            // customizer for Interconnection, since it's not a DataObject
            // FIXME we might need a choice customizer
            // THis choice is already from augment, so its probably not
            // possible to augment augmented choice
            LOG.error("Unable to handle subnet of type {}", subnet.getClass());
            throw new WriteFailedException(id, "Unable to handle subnet of type " + subnet.getClass());
        }
    }

    private void setNetmaskSubnet(final boolean add, final InstanceIdentifier<Address> id, final String name,
                                  final int swIfc,
                                  final Address ipv4Addr, final Netmask subnet) throws WriteFailedException {
        LOG.debug("Setting Subnet(subnet-mask) for interface: {}, {}. Subnet: {}, Ipv4: {}", name, swIfc, subnet,
            ipv4Addr);

        byte[] addr = TranslateUtils.ipv4AddressNoZoneToArray(ipv4Addr.getIp());
        final DottedQuad netmask = subnet.getNetmask();

        checkNotNull(addr, "Null address");
        checkNotNull(netmask, "Null netmask");

        // find netmask bit-length
        final short subnetLength = getSubnetMaskLength(netmask.getValue());
        PrefixLengthBuilder lengthBuilder = new PrefixLengthBuilder().setPrefixLength(subnetLength);

        setPrefixLengthSubnet(add, id, name, swIfc, ipv4Addr, lengthBuilder.build());
    }

    /**
     * Returns the prefix size in bits of the specified subnet mask. Example: For the subnet mask 255.255.255.128 it
     * returns 25 while for 255.0.0.0 it returns 8. If the passed subnetMask array is not complete or contains not only
     * leading ones, IllegalArgumentExpression is thrown
     *
     * @param mask the subnet mask in dot notation 255.255.255.255
     * @return the prefix length as number of bits
     */
    private static short getSubnetMaskLength(final String mask) {
        String[] maskParts = mask.split("\\.");

        final int DOTTED_QUAD_MASK_LENGHT = 4;
        final int IPV4_ADDRESS_PART_BITS_COUNT = 8;
        final int NETMASK_PART_LIMIT = 256; // 2 power to 8

        checkArgument(maskParts.length == DOTTED_QUAD_MASK_LENGHT,
            "Network mask %s is not in Quad Dotted Decimal notation!", mask);

        long maskAsNumber = 0;
        for (int i = 0; i < DOTTED_QUAD_MASK_LENGHT; i++) {
            maskAsNumber <<= IPV4_ADDRESS_PART_BITS_COUNT;
            int value = Integer.parseInt(maskParts[i]);
            checkArgument(value < NETMASK_PART_LIMIT, "Network mask %s contains invalid number(s) over 255!", mask);
            checkArgument(value >= 0, "Network mask %s contains invalid negative number(s)!", mask);
            maskAsNumber += value;
        }

        String bits = Long.toBinaryString(maskAsNumber);
        checkArgument(bits.length() == IPV4_ADDRESS_PART_BITS_COUNT * DOTTED_QUAD_MASK_LENGHT,
            "Incorrect network mask %s", mask);
        final int leadingOnes = bits.indexOf('0');
        checkArgument(leadingOnes != -1, "Broadcast address %s is not allowed!", mask);
        checkArgument(bits.substring(leadingOnes).indexOf('1') == -1,
            "Non-contiguous network mask %s is not allowed!", mask);
        return (short) leadingOnes;
    }

    private void setPrefixLengthSubnet(boolean add, final InstanceIdentifier<Address> id, final String name,
                                       final int swIfc,
                                       final Address address, final PrefixLength subnet) throws WriteFailedException {
        try {
            LOG.debug("Setting Subnet(prefix-length) for interface: {}, {}. Subnet: {}, Address: {}", name, swIfc,
                subnet, address);

            final Short plen = subnet.getPrefixLength();
            checkArgument(plen > 0, "Invalid length");

            final byte[] addr = TranslateUtils.ipv4AddressNoZoneToArray(address.getIp());
            checkNotNull(addr, "Null address");

            final CompletionStage<SwInterfaceAddDelAddressReply> swInterfaceAddDelAddressReplyCompletionStage =
                getFutureJVpp()
                    .swInterfaceAddDelAddress(
                        getSwInterfaceAddDelAddressRequest(swIfc, TranslateUtils.booleanToByte(add) /* isAdd */,
                            (byte) 0 /* isIpv6 */, (byte) 0 /* delAll */, plen.byteValue(), addr));

            TranslateUtils.getReply(swInterfaceAddDelAddressReplyCompletionStage.toCompletableFuture());

            LOG.debug("Subnet(prefix-length) set successfully for interface: {}, {},  Subnet: {}, Address: {}", name,
                swIfc, subnet, address);
        } catch (VppBaseCallException e) {
            LOG.warn("Failed to set Subnet(prefix-length) for interface: {}, {},  Subnet: {}, Address: {}", name, swIfc,
                subnet, address);
            throw new WriteFailedException(id, "Unable to handle subnet of type " + subnet.getClass(), e);
        }
    }

    private SwInterfaceAddDelAddress getSwInterfaceAddDelAddressRequest(final int swIfc, final byte isAdd,
                                                                        final byte ipv6, final byte deleteAll,
                                                                        final byte length, final byte[] addr) {
        final SwInterfaceAddDelAddress swInterfaceAddDelAddress = new SwInterfaceAddDelAddress();
        swInterfaceAddDelAddress.swIfIndex = swIfc;
        swInterfaceAddDelAddress.isAdd = isAdd;
        swInterfaceAddDelAddress.isIpv6 = ipv6;
        swInterfaceAddDelAddress.delAll = deleteAll;
        swInterfaceAddDelAddress.address = addr;
        swInterfaceAddDelAddress.addressLength = length;
        return swInterfaceAddDelAddress;
    }

}
