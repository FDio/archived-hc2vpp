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

package io.fd.hc2vpp.v3po.read;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.read.cache.InterfaceCacheDumpManager;
import io.fd.hc2vpp.v3po.util.SubInterfaceUtils;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.jvpp.core.dto.SwInterfaceDetails;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.SubInterfaceStatus;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.SubInterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.match.attributes.match.type.DefaultBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.match.attributes.match.type.UntaggedBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.match.attributes.match.type.vlan.tagged.VlanTaggedBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.base.attributes.Match;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.base.attributes.MatchBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.base.attributes.Tags;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.base.attributes.TagsBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.base.attributes.tags.Tag;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.base.attributes.tags.TagBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.base.attributes.tags.TagKey;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.CVlan;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.Dot1qTagVlanType;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.Dot1qVlanId;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.SVlan;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.or.any.Dot1qTag;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.or.any.Dot1qTagBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading sub interfaces form the VPP.
 */
public class SubInterfaceCustomizer extends FutureJVppCustomizer
        implements InitializingListReaderCustomizer<SubInterface, SubInterfaceKey, SubInterfaceBuilder>,
        ByteDataTranslator,
        InterfaceDataTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(SubInterfaceCustomizer.class);
    private static final Dot1qTag.VlanId ANY_VLAN_ID = new Dot1qTag.VlanId(Dot1qTag.VlanId.Enumeration.Any);
    private final NamingContext interfaceContext;
    private final InterfaceCacheDumpManager dumpManager;

    public SubInterfaceCustomizer(@Nonnull final FutureJVppCore jvpp,
                                  @Nonnull final NamingContext interfaceContext,
                                  @Nonnull final InterfaceCacheDumpManager dumpManager) {
        super(jvpp);
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
        this.dumpManager = checkNotNull(dumpManager, "dumpManager should not be null");
    }

    private static String getSubInterfaceName(final InstanceIdentifier<SubInterface> id) {
        return SubInterfaceUtils.getSubInterfaceName(id.firstKeyOf(Interface.class).getName(),
                Math.toIntExact(id.firstKeyOf(id.getTargetType()).getIdentifier()));
    }

    private static Tag buildTag(final short index, final Class<? extends Dot1qTagVlanType> tagType,
                                final Dot1qTag.VlanId vlanId) {
        TagBuilder tag = new TagBuilder();
        tag.setIndex(index);
        tag.withKey(new TagKey(index));
        final Dot1qTagBuilder dtag = new Dot1qTagBuilder();
        dtag.setTagType(tagType);
        dtag.setVlanId(vlanId);
        tag.setDot1qTag(dtag.build());
        return tag.build();
    }

    private static Dot1qTag.VlanId buildVlanId(final short vlanId) {
        // treat vlanId as unsigned value:
        return new Dot1qTag.VlanId(new Dot1qVlanId(0xffff & vlanId));
    }

    @Nonnull
    @Override
    public List<SubInterfaceKey> getAllIds(@Nonnull final InstanceIdentifier<SubInterface> id,
                                           @Nonnull final ReadContext context) throws ReadFailedException {
        // Relying here that parent InterfaceCustomizer was invoked first (PREORDER)
        // to fill in the context with initial ifc mapping
        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final String ifaceName = key.getName();
        final int ifaceId = interfaceContext.getIndex(ifaceName, context.getMappingContext());

        final List<SubInterfaceKey> interfacesKeys = dumpManager.getInterfaces(id,context)
                .filter(Objects::nonNull)
                // accept only sub-interfaces for current iface:
                .filter(elt -> isSubInterface(elt) && elt.supSwIfIndex == ifaceId)
                .map(details -> new SubInterfaceKey(new Long(details.subId)))
                .collect(Collectors.toList());

        LOG.debug("Sub-interfaces of {} found in VPP: {}", ifaceName, interfacesKeys);
        return interfacesKeys;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder,
                      @Nonnull final List<SubInterface> readData) {
        ((SubInterfacesBuilder) builder).setSubInterface(readData);
    }

    @Nonnull
    @Override
    public SubInterfaceBuilder getBuilder(@Nonnull final InstanceIdentifier<SubInterface> id) {
        return new SubInterfaceBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<SubInterface> id,
                                      @Nonnull final SubInterfaceBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        final String subInterfaceName = getSubInterfaceName(id);
        LOG.debug("Reading attributes for sub interface: {}", subInterfaceName);

        final SwInterfaceDetails iface = dumpManager.getInterfaceDetail(id, ctx, subInterfaceName);
        LOG.debug("VPP sub-interface details: {}", iface);

        checkState(isSubInterface(iface), "Interface returned by the VPP is not a sub-interface");

        builder.setIdentifier((long) iface.subId);
        builder.withKey(new SubInterfaceKey(builder.getIdentifier()));

        // sub-interface-base-attributes:
        builder.setTags(readTags(iface));
        builder.setMatch(readMatch(iface));

        // sub-interface-operational-attributes:
        builder.setAdminStatus(1 == iface.adminUpDown
                ? SubInterfaceStatus.Up
                : SubInterfaceStatus.Down);
        builder.setOperStatus(1 == iface.linkUpDown
                ? SubInterfaceStatus.Up
                : SubInterfaceStatus.Down);
        builder.setIfIndex(vppIfIndexToYang(iface.swIfIndex));
        if (iface.l2AddressLength == 6) {
            builder.setPhysAddress(new PhysAddress(vppPhysAddrToYang(iface.l2Address)));
        }
        if (0 != iface.linkSpeed) {
            builder.setSpeed(vppInterfaceSpeedToYang(iface.linkSpeed));
        }
    }

    private Tags readTags(final SwInterfaceDetails iface) {
        final TagsBuilder tags = new TagsBuilder();
        final List<Tag> list = new ArrayList<>();
        if (iface.subNumberOfTags > 0) {
            if (iface.subOuterVlanIdAny == 1) {
                list.add(buildTag((short) 0, SVlan.class, ANY_VLAN_ID));
            } else {
                list.add(buildTag((short) 0, SVlan.class, buildVlanId(iface.subOuterVlanId)));
            }
            // inner tag (customer tag):
            if (iface.subNumberOfTags == 2) {
                if (iface.subInnerVlanIdAny == 1) {
                    list.add(buildTag((short) 1, CVlan.class, ANY_VLAN_ID));
                } else {
                    list.add(buildTag((short) 1, CVlan.class, buildVlanId(iface.subInnerVlanId)));
                }
            }
        }
        tags.setTag(list);
        return tags.build();
    }

    private Match readMatch(final SwInterfaceDetails iface) {
        final MatchBuilder match = new MatchBuilder();
        if (iface.subDefault == 1) {
            match.setMatchType(new DefaultBuilder().build());
        } else if (iface.subNumberOfTags == 0) {
            match.setMatchType(new UntaggedBuilder().build());
        } else {
            final VlanTaggedBuilder tagged = new VlanTaggedBuilder();
            tagged.setMatchExactTags(byteToBoolean(iface.subExactMatch));
            match.setMatchType(
                    new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.match.attributes.match.type.VlanTaggedBuilder()
                            .setVlanTagged(tagged.build()).build());
        }
        return match.build();
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterface> init(
            @Nonnull final InstanceIdentifier<SubInterface> id, @Nonnull final SubInterface readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
                new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterfaceBuilder()
                        .setEnabled(SubInterfaceStatus.Up.equals(readValue.getAdminStatus()))
                        .setIdentifier(readValue.getIdentifier())
                        .setMatch(readValue.getMatch())
                        .setTags(readValue.getTags())
                        .setVlanType(readValue.getVlanType())
                        .build());
    }

    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterface> getCfgId(
            final InstanceIdentifier<SubInterface> id) {
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                .augmentation(SubinterfaceAugmentation.class)
                .child(SubInterfaces.class)
                .child(org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterface.class,
                        new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterfaceKey(
                                id.firstKeyOf(SubInterface.class).getIdentifier()));
    }
}
