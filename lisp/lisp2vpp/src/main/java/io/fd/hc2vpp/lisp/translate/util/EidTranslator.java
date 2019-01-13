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

package io.fd.hc2vpp.lisp.translate.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.InetAddresses.forString;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV4;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV4_PREFIX;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV6;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV6_PREFIX;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.MAC;
import static java.lang.Integer.parseInt;

import inet.ipaddr.IPAddress;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import java.util.Arrays;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.adjacencies.grouping.adjacencies.adjacency.LocalEid;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.adjacencies.grouping.adjacencies.adjacency.RemoteEid;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4PrefixAfi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv6Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv6PrefixAfi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.LispAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.MacAfi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv6PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Mac;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.MacBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.slf4j.Logger;


/**
 * Trait providing converting logic for eid's
 */
// TODO - HC2VPP-149 - restructuralize code
public interface EidTranslator extends AddressTranslator, EidMetadataProvider {

    EidTranslator INSTANCE = new EidTranslator(){};

    byte DEFAULT_V4_PREFIX = 32;
    byte DEFAULT_V6_PREFIX = (byte) 128;

    default byte getPrefixLength(LocalEid address) {
        return resolverPrefixLength(address.getAddress());
    }

    default byte getPrefixLength(RemoteEid address) {
        return resolverPrefixLength(address.getAddress());
    }

    default byte getPrefixLength(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid address) {
        return resolverPrefixLength(address.getAddress());
    }

    default byte getPrefixLength(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.local.mapping.Eid address) {
        return resolverPrefixLength(address.getAddress());
    }

    default byte getPrefixLength(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.Eid address) {
        return resolverPrefixLength(address.getAddress());
    }

    default byte getPrefixLength(org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocalEid address){
        return resolverPrefixLength(address.getAddress());
    }

    default byte getPrefixLength(org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEid address){
        return resolverPrefixLength(address.getAddress());
    }

    static byte resolverPrefixLength(Address address) {

        switch (resolveType(address)) {
            case IPV4:
                return DEFAULT_V4_PREFIX;
            case IPV6:
                return DEFAULT_V6_PREFIX;
            case MAC:
                return 0;
            case IPV4_PREFIX:
                return extractPrefix(Ipv4Prefix.class.cast(address).getIpv4Prefix().getValue());
            case IPV6_PREFIX:
                return extractPrefix(Ipv6Prefix.class.cast(address).getIpv6Prefix().getValue());
            default:
                throw new IllegalArgumentException("Illegal type");
        }
    }

    static byte extractPrefix(final String data) {
        return Byte.valueOf(data.substring(data.indexOf('/') + 1));
    }

    default org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocalEid getArrayAsGpeLocalEid(
            @Nonnull final EidType type, final byte[] address, final byte prefix, final int vni) {
        final Eid eid = getArrayAsEidLocal(type, address, prefix, vni);

        return new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocalEidBuilder()
                .setAddress(eid.getAddress())
                .setAddressType(eid.getAddressType())
                .setVirtualNetworkId(eid.getVirtualNetworkId())
                .build();
    }

    default org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEid getArrayAsGpeRemoteEid(
            @Nonnull final EidType type, final byte[] address, final byte prefix, final int vni) {
        final Eid eid = getArrayAsEidLocal(type, address, prefix, vni);

        return new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEidBuilder()
                .setAddress(eid.getAddress())
                .setAddressType(eid.getAddressType())
                .setVirtualNetworkId(eid.getVirtualNetworkId())
                .build();
    }

    default Eid getArrayAsEidLocal(@Nonnull final EidType type, final byte[] address, final byte prefix,
                                   final int vni) {

        switch (type) {
            case IPV4: {
                // vpp does no have separate constant for prefix based ,so if prefix is different than
                // default, map it to prefix type. Same in any other logic switched by eid type
                return prefix != DEFAULT_V4_PREFIX
                        ? newLocalEidBuilder(Ipv4PrefixAfi.class, vni).setAddress(
                        new Ipv4PrefixBuilder().setIpv4Prefix(v4Prefix(address, prefix)).build()).build()
                        : newLocalEidBuilder(Ipv4Afi.class, vni).setAddress(
                                new Ipv4Builder().setIpv4(arrayToIpv4AddressNoZone(address)).build())
                                .build();
            }
            case IPV6: {
                return prefix != DEFAULT_V6_PREFIX
                        ? newLocalEidBuilder(Ipv6PrefixAfi.class, vni).setAddress(
                        new Ipv6PrefixBuilder().setIpv6Prefix(v6Prefix(address, prefix)).build()).build()
                        : newLocalEidBuilder(Ipv6Afi.class, vni).setAddress(
                                new Ipv6Builder().setIpv6(arrayToIpv6AddressNoZone(address)).build())
                                .build();
            }
            case MAC: {
                return newLocalEidBuilder(MacAfi.class, vni).setAddress(
                        new MacBuilder().setMac(new MacAddress(byteArrayToMacSeparated(address)))
                                .build()).build();
            }
            case IPV4_PREFIX: {
                return newLocalEidBuilder(Ipv4PrefixAfi.class, vni).setAddress(
                        new Ipv4PrefixBuilder().setIpv4Prefix(v4Prefix(address, prefix)).build()).build();
            }
            case IPV6_PREFIX: {
                return newLocalEidBuilder(Ipv6PrefixAfi.class, vni).setAddress(
                        new Ipv6PrefixBuilder().setIpv6Prefix(v6Prefix(address, prefix)).build()).build();
            }
            default: {
                throw new IllegalStateException("Unknown type detected");
            }
        }
    }

    static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix v4Prefix(
            final byte[] address, final byte prefix) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix(
                prefixValue(INSTANCE.arrayToIpv4AddressNoZone(address).getValue(), String.valueOf(prefix)));
    }

    static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix v6Prefix(
            final byte[] address, final byte prefix) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix(
                prefixValue(INSTANCE.arrayToIpv6AddressNoZone(address).getValue(), String.valueOf(prefix)));
    }

    static String prefixValue(final String prefix, final String suffix) {
        // normalize prefix based address to prevent duplicates
        IPAddress normalizedForm = IPAddress.from(forString(prefix)).toSubnet(parseInt(suffix));

        return normalizedForm.toCompressedString();
    }

    default org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.Eid getArrayAsEidRemote(
            @Nonnull final EidType type, final byte[] address, final byte prefix, final int vni) {

        switch (type) {
            case IPV4: {
                return prefix != DEFAULT_V4_PREFIX
                        ? newRemoteEidBuilder(Ipv4PrefixAfi.class, vni).setAddress(
                        new Ipv4PrefixBuilder().setIpv4Prefix(v4Prefix(address, prefix)).build()).build()
                        : newRemoteEidBuilder(Ipv4Afi.class, vni)
                                .setAddress(new Ipv4Builder().setIpv4(arrayToIpv4AddressNoZone(address)).build())
                                .build();
            }
            case IPV6: {
                return prefix != DEFAULT_V6_PREFIX
                        ? newRemoteEidBuilder(Ipv6PrefixAfi.class, vni).setAddress(
                        new Ipv6PrefixBuilder().setIpv6Prefix(v6Prefix(address, prefix)).build()).build()
                        : newRemoteEidBuilder(Ipv6Afi.class, vni)
                                .setAddress(new Ipv6Builder().setIpv6(arrayToIpv6AddressNoZone(address)).build())
                                .build();
            }
            case MAC: {
                return newRemoteEidBuilder(MacAfi.class, vni)
                        .setAddress(
                                new MacBuilder().setMac(new MacAddress(byteArrayToMacSeparated(address)))
                                        .build()).build();
            }
            case IPV4_PREFIX: {
                return newRemoteEidBuilder(Ipv4PrefixAfi.class, vni).setAddress(
                        new Ipv4PrefixBuilder().setIpv4Prefix(v4Prefix(address, prefix)).build()).build();
            }
            case IPV6_PREFIX: {
                return newRemoteEidBuilder(Ipv6PrefixAfi.class, vni).setAddress(
                        new Ipv6PrefixBuilder().setIpv6Prefix(v6Prefix(address, prefix)).build()).build();
            }
            default: {
                throw new IllegalStateException("Unknown type detected");
            }
        }
    }

    default LocalEid getArrayAsLocalEid(@Nonnull final EidType type, final byte[] address, final byte prefix,
                                        final int vni) {
        switch (type) {
            case IPV4: {
                if (prefix != DEFAULT_V4_PREFIX) {
                    return newEidBuilderLocal(Ipv4PrefixAfi.class, vni).setAddress(
                            new Ipv4PrefixBuilder().setIpv4Prefix(v4Prefix(address, prefix)).build()).build();
                }
                return newEidBuilderLocal(Ipv4Afi.class, vni)
                        .setAddress(
                                new Ipv4Builder().setIpv4(arrayToIpv4AddressNoZone(address))
                                        .build())
                        .build();
            }
            case IPV6: {
                if (prefix != DEFAULT_V6_PREFIX) {
                    return newEidBuilderLocal(Ipv6PrefixAfi.class, vni).setAddress(
                            new Ipv6PrefixBuilder().setIpv6Prefix(v6Prefix(address, prefix)).build()).build();
                }
                return newEidBuilderLocal(Ipv6Afi.class, vni)
                        .setAddress(
                                new Ipv6Builder().setIpv6(arrayToIpv6AddressNoZone(address))
                                        .build())
                        .build();
            }
            case MAC: {
                return newEidBuilderLocal(MacAfi.class, vni)
                        .setAddress(
                                new MacBuilder().setMac(new MacAddress(byteArrayToMacSeparated(address)))
                                        .build()).build();
            }
            case IPV4_PREFIX: {
                return newEidBuilderLocal(Ipv4PrefixAfi.class, vni).setAddress(
                        new Ipv4PrefixBuilder().setIpv4Prefix(v4Prefix(address, prefix)).build()).build();
            }
            case IPV6_PREFIX: {
                return newEidBuilderLocal(Ipv6PrefixAfi.class, vni).setAddress(
                        new Ipv6PrefixBuilder().setIpv6Prefix(v6Prefix(address, prefix)).build()).build();
            }
            default: {
                throw new IllegalStateException("Unknown type detected");
            }
        }
    }

    default RemoteEid getArrayAsRemoteEid(@Nonnull final EidType type, final byte[] address, final byte prefix,
                                          final int vni) {
        switch (type) {
            case IPV4: {
                return (prefix != DEFAULT_V4_PREFIX)
                        ? newEidBuilderRemote(Ipv4PrefixAfi.class, vni).setAddress(
                        new Ipv4PrefixBuilder().setIpv4Prefix(v4Prefix(address, prefix)).build()).build()
                        : newEidBuilderRemote(Ipv4Afi.class, vni)
                                .setAddress(new Ipv4Builder().setIpv4(arrayToIpv4AddressNoZone(address)).build())
                                .build();
            }
            case IPV6: {
                return prefix != DEFAULT_V6_PREFIX
                        ? newEidBuilderRemote(Ipv6PrefixAfi.class, vni)
                        .setAddress(new Ipv6PrefixBuilder().setIpv6Prefix(v6Prefix(address, prefix)).build()).build()
                        : newEidBuilderRemote(Ipv6Afi.class, vni)
                                .setAddress(new Ipv6Builder().setIpv6(arrayToIpv6AddressNoZone(address)).build())
                                .build();
            }
            case MAC: {
                return newEidBuilderRemote(MacAfi.class, vni)
                        .setAddress(
                                new MacBuilder().setMac(new MacAddress(byteArrayToMacSeparated(address)))
                                        .build()).build();
            }
            case IPV4_PREFIX: {
                return newEidBuilderRemote(Ipv4PrefixAfi.class, vni).setAddress(
                        new Ipv4PrefixBuilder().setIpv4Prefix(v4Prefix(address, prefix)).build()).build();
            }
            case IPV6_PREFIX: {
                return newEidBuilderRemote(Ipv6PrefixAfi.class, vni).setAddress(
                        new Ipv6PrefixBuilder().setIpv6Prefix(v6Prefix(address, prefix)).build()).build();
            }
            default: {
                throw new IllegalStateException("Unknown type detected");
            }
        }
    }

    default String getArrayAsEidString(final EidType type, final byte[] address, final byte prefix) {
        switch (type) {
            case IPV4: {
                return prefix != DEFAULT_V4_PREFIX
                        ? v4Prefix(address, prefix).getValue()
                        : arrayToIpv4AddressNoZone(address).getValue();
            }
            case IPV6: {
                return prefix != DEFAULT_V6_PREFIX
                        ? v6Prefix(address, prefix).getValue()
                        : arrayToIpv6AddressNoZone(address).getValue();
            }
            case MAC: {
                //as wrong as it looks ,its right(second param is not end index,but count)
                return byteArrayToMacSeparated(Arrays.copyOfRange(address, 0, 6));
            }
            case IPV4_PREFIX: {
                return v4Prefix(address, prefix).getValue();
            }
            case IPV6_PREFIX: {
                return v6Prefix(address, prefix).getValue();
            }
            default: {
                throw new IllegalStateException("Unknown type detected");
            }
        }
    }

    default EidType getEidType(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEid address) {
        checkNotNull(address, "Address cannot be null");

        return resolveType(address.getAddress());
    }

    default EidType getEidType(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocalEid address) {
        checkNotNull(address, "Address cannot be null");

        return resolveType(address.getAddress());
    }

    default EidType getEidType(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid address) {
        checkNotNull(address, "SimpleAddress cannot be null");

        return resolveType(address.getAddress());
    }

    default EidType getEidType(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.local.mapping.Eid address) {
        checkNotNull(address, "SimpleAddress cannot be null");

        return resolveType(address.getAddress());
    }


    default EidType getEidType(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.Eid address) {
        checkNotNull(address, "Address cannot be null");

        return resolveType(address.getAddress());
    }


    default EidType getEidType(final LocalEid address) {
        checkNotNull(address, "Address cannot be null");

        return resolveType(address.getAddress());
    }

    default EidType getEidType(final RemoteEid address) {
        checkNotNull(address, "Address cannot be null");

        return resolveType(address.getAddress());
    }

    static EidType resolveType(
            Address address) {

        if (address instanceof Ipv4) {
            return IPV4;
        } else if (address instanceof Ipv6) {
            return IPV6;
        } else if (address instanceof Mac) {
            return MAC;
        } else if (address instanceof Ipv4Prefix) {
            return IPV4_PREFIX;
        } else if (address instanceof Ipv6Prefix) {
            return IPV6_PREFIX;
        } else {
            throw new IllegalStateException("Unknown type detected");
        }
    }

    default byte[] getEidAsByteArray(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEid address) {
        checkNotNull(address, "Eid cannot be null");

        return resolveByteArray(getEidType(address), address.getAddress());
    }

    default byte[] getEidAsByteArray(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocalEid address) {
        checkNotNull(address, "Eid cannot be null");

        return resolveByteArray(getEidType(address), address.getAddress());
    }

    default byte[] getEidAsByteArray(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid address) {
        checkNotNull(address, "Eid cannot be null");

        return resolveByteArray(getEidType(address), address.getAddress());
    }

    default byte[] getEidAsByteArray(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.local.mapping.Eid address) {
        checkNotNull(address, "Eid cannot be null");

        return resolveByteArray(getEidType(address), address.getAddress());
    }

    default byte[] getEidAsByteArray(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.Eid address) {
        checkNotNull(address, "Eid cannot be null");

        return resolveByteArray(getEidType(address), address.getAddress());
    }

    default byte[] getEidAsByteArray(final LocalEid address) {
        checkNotNull(address, "Eid cannot be null");

        return resolveByteArray(getEidType(address), address.getAddress());
    }


    default byte[] getEidAsByteArray(final RemoteEid address) {
        checkNotNull(address, "Eid cannot be null");

        return resolveByteArray(getEidType(address), address.getAddress());
    }

    default byte[] resolveByteArray(EidType type, Address address) {
        switch (type) {
            case IPV4:
                return ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(((Ipv4) address).getIpv4()));
            case IPV6:
                return ipv6AddressNoZoneToArray(new Ipv6AddressNoZone(((Ipv6) address).getIpv6()));
            case MAC:
                return parseMac(((Mac) address).getMac().getValue());
            case IPV4_PREFIX:
                return ipv4AddressPrefixToArray(v4LispPrefixToInetPrefix(Ipv4Prefix.class.cast(address)));
            case IPV6_PREFIX:
                return ipv6AddressPrefixToArray(v6LispPrefixToInetPrefix(Ipv6Prefix.class.cast(address)));
            default:
                throw new IllegalArgumentException("Unsupported type");
        }
    }

    default Address normalizeIfPrefixBased(Address address){
        if(address instanceof Ipv4Prefix){
            final String[] parts = ((Ipv4Prefix) address).getIpv4Prefix().getValue().split("/");

            return  new Ipv4PrefixBuilder().setIpv4Prefix(
                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix(
                            prefixValue(parts[0], parts[1])))
                    .build();
        }

        if (address instanceof Ipv6Prefix){
            final String[] parts = ((Ipv6Prefix) address).getIpv6Prefix().getValue().split("/");

            return  new Ipv6PrefixBuilder().setIpv6Prefix(
                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix(
                            prefixValue(parts[0], parts[1]))).build();
        }

        // if not prefix based, does nothing
        return address;
    }

    static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix v4LispPrefixToInetPrefix(
            final Ipv4Prefix prefix) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix(
                prefix.getIpv4Prefix().getValue());
    }

    static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix v6LispPrefixToInetPrefix(
            final Ipv6Prefix prefix) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix(
                prefix.getIpv6Prefix().getValue());
    }

    default boolean compareEids(
            LispAddress first,
            LispAddress second) {

        return compareAddresses(checkNotNull(first, "First eid is null").getAddress(),
                checkNotNull(second, "Second eid is null").getAddress());
    }

    default boolean compareAddresses(Address firstAddress, Address secondAddress) {

        checkNotNull(firstAddress, "First address is null");
        checkNotNull(secondAddress, "Second address is null");

        if (firstAddress instanceof Ipv4 && secondAddress instanceof Ipv4) {
            return ((Ipv4) firstAddress).getIpv4().getValue().equals(((Ipv4) secondAddress).getIpv4().getValue());
        }

        if (firstAddress instanceof Ipv6 && secondAddress instanceof Ipv6) {
            return ((Ipv6) firstAddress).getIpv6().getValue().equals(((Ipv6) secondAddress).getIpv6().getValue());
        }

        if (firstAddress instanceof Mac && secondAddress instanceof Mac) {
            return ((Mac) firstAddress).getMac().getValue().equals(((Mac) secondAddress).getMac().getValue());
        }

        if (firstAddress instanceof Ipv4Prefix && secondAddress instanceof Ipv4Prefix) {

            final String firstPrefix = ((Ipv4Prefix) firstAddress).getIpv4Prefix().getValue();
            final String secondPrefix = ((Ipv4Prefix) secondAddress).getIpv4Prefix().getValue();

            // for ex. 192.168.2.1/24 and 192.168.2.2/24 will be optimized to
            // 192.168.2.0/24
            return isSameSubnet(firstPrefix, secondPrefix);
        }

        if (firstAddress instanceof Ipv6Prefix && secondAddress instanceof Ipv6Prefix) {
            final String firstPrefix = ((Ipv6Prefix) firstAddress).getIpv6Prefix().getValue();
            final String secondPrefix = ((Ipv6Prefix) secondAddress).getIpv6Prefix().getValue();

            // same here
            return isSameSubnet(firstPrefix, secondPrefix);
        }

        return false;
    }

    /**
     * Configuration data store whatever value is put, so it can be non-normalized, but
     * vpp optimize all eid prefix based values, returns true if such case
     */
    default void checkIgnoredSubnetUpdate(@Nonnull final Address dataBefore,
                                             @Nonnull final Address dataAfter,
                                             @Nonnull Logger logger) {
        boolean isSameSubnet = false;
        if (dataBefore instanceof Ipv4Prefix && dataAfter instanceof Ipv4Prefix) {
            isSameSubnet = isSameSubnet(((Ipv4Prefix) dataBefore).getIpv4Prefix().getValue(),
                    ((Ipv4Prefix) dataAfter).getIpv4Prefix().getValue());
        }

        if (dataBefore instanceof Ipv6Prefix && dataAfter instanceof Ipv6Prefix) {
            isSameSubnet = isSameSubnet(((Ipv6Prefix) dataBefore).getIpv6Prefix().getValue(),
                    ((Ipv6Prefix) dataAfter).getIpv6Prefix().getValue());
        }

        if (isSameSubnet) {
            logger.warn("Attempt to update address within same subnet detected, ignoring[{} vs {}]", dataBefore,
                    dataAfter);
            return;
        }

        throw new UnsupportedOperationException("Operation not supported");
    }

    static boolean isSameSubnet(final String firstPrefix, final String secondPrefix) {
        final String[] firstPrefixParts = getPrefixParts(firstPrefix);
        final String[] secondPrefixParts = getPrefixParts(secondPrefix);

        IPAddress firstAddress =
                IPAddress.from(forString(firstPrefixParts[0])).toSubnet(parseInt(firstPrefixParts[1]));
        IPAddress secondAddress =
                IPAddress.from(forString(secondPrefixParts[0])).toSubnet(parseInt(secondPrefixParts[1]));

        return firstAddress.compareTo(secondAddress) == 0;
    }

    static String[] getPrefixParts(final String prefixString) {
        final String[] split = prefixString.split("/");
        checkArgument(split.length == 2, "%s is not a valid ip prefix", prefixString);
        return split;
    }
}
