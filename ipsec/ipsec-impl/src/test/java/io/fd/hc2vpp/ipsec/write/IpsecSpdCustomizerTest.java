/*
 * Copyright (c) 2019 PANTHEON.tech.
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

package io.fd.hc2vpp.ipsec.write;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.ipsec.helpers.SchemaContextTestHelper;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.IpsecSpdAddDel;
import io.fd.jvpp.core.dto.IpsecSpdAddDelReply;
import io.fd.jvpp.core.dto.IpsecSpdEntryAddDel;
import io.fd.jvpp.core.dto.IpsecSpdEntryAddDelReply;
import io.fd.jvpp.core.types.IpsecSpdAction;
import io.fd.jvpp.core.types.IpsecSpdEntry;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecSpdEntriesAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.Ipsec;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.Spd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.SpdBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.SpdKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.spd.SpdEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.spd.SpdEntriesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class IpsecSpdCustomizerTest extends WriterCustomizerTest implements SchemaContextTestHelper,
        ByteDataTranslator, Ipv4Translator, Ipv6Translator {

    private static final int SPD_ID = 10;
    private static final String IPSEC_PATH = "/hc2vpp-ietf-ipsec:ipsec";
    private IpsecSpdCustomizer customizer;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new IpsecSpdCustomizer(api);
        when(api.ipsecSpdAddDel(any())).thenReturn(future(new IpsecSpdAddDelReply()));
        when(api.ipsecSpdEntryAddDel(any())).thenReturn(future(new IpsecSpdEntryAddDelReply()));
    }

    @Test
    public void testWrite(@InjectTestData(resourcePath = "/spdEntries/addDelSpd.json", id = IPSEC_PATH) Ipsec ipsec)
            throws WriteFailedException {
        Spd spd = ipsec.getSpd().get(0);
        customizer.writeCurrentAttributes(getSpdId(SPD_ID), spd, writeContext);
        final IpsecSpdAddDel createSpdRequest = new IpsecSpdAddDel();
        createSpdRequest.isAdd = BYTE_TRUE;
        createSpdRequest.spdId = SPD_ID;

        verify(api).ipsecSpdAddDel(createSpdRequest);
        verify(api).ipsecSpdEntryAddDel(translateSpdEntry(spd.getSpdEntries().get(0), SPD_ID, true));
        verify(api).ipsecSpdEntryAddDel(translateSpdEntry(spd.getSpdEntries().get(1), SPD_ID, true));
    }

    @Test
    public void testUpdate(
            @InjectTestData(resourcePath = "/spdEntries/addDelSpd_before.json", id = IPSEC_PATH) Ipsec ipsecBefore,
            @InjectTestData(resourcePath = "/spdEntries/addDelSpd_after.json", id = IPSEC_PATH) Ipsec ipsecAfter)
            throws WriteFailedException {
        Spd before = ipsecBefore.getSpd().get(0);
        Spd after = ipsecAfter.getSpd().get(0);
        customizer.updateCurrentAttributes(getSpdId(SPD_ID), before, after, writeContext);
        verify(api).ipsecSpdEntryAddDel(translateSpdEntry(after.getSpdEntries().get(0), SPD_ID, true));
    }

    @Test
    public void testDelete()
            throws WriteFailedException {
        SpdBuilder spdBuilder = new SpdBuilder();
        spdBuilder.setSpdId(SPD_ID)
                .withKey(new SpdKey(SPD_ID))
                .setSpdEntries(Collections.singletonList(new SpdEntriesBuilder().build()));
        customizer.deleteCurrentAttributes(getSpdId(SPD_ID), spdBuilder.build(), writeContext);
        IpsecSpdAddDel request = new IpsecSpdAddDel();
        request.spdId = SPD_ID;
        request.isAdd = BYTE_FALSE;
        verify(api).ipsecSpdAddDel(request);
    }

    private InstanceIdentifier<Spd> getSpdId(final int spdId) {
        return InstanceIdentifier.create(Ipsec.class).child(Spd.class, new SpdKey(spdId));
    }

    private IpsecSpdEntryAddDel translateSpdEntry(final SpdEntries entry, int spdId, boolean isAdd) {
        IpsecSpdEntryAddDel request = new IpsecSpdEntryAddDel();
        request.entry = new IpsecSpdEntry();
        request.entry.spdId = spdId;
        request.isAdd = isAdd
                ? BYTE_TRUE
                : BYTE_FALSE;
        IpsecSpdEntriesAugmentation aug = entry.augmentation(IpsecSpdEntriesAugmentation.class);
        if (aug != null) {

            if (aug.getDirection() != null) {
                request.entry.isOutbound = (byte) aug.getDirection().getIntValue();
            }

            if (aug.getPriority() != null) {
                request.entry.priority = aug.getPriority();
            }

            if (aug.getOperation() != null) {
                final String operation = aug.getOperation().getName();
                if (operation.equalsIgnoreCase("bypass")) {
                    request.entry.policy = IpsecSpdAction.IPSEC_API_SPD_ACTION_BYPASS;
                } else if (operation.equalsIgnoreCase("discard")) {
                    request.entry.policy = IpsecSpdAction.IPSEC_API_SPD_ACTION_DISCARD;
                } else if (operation.equalsIgnoreCase("protect")) {
                    request.entry.policy = IpsecSpdAction.IPSEC_API_SPD_ACTION_PROTECT;
                }
            }

            if (aug.getLaddrStart() != null) {
                if (aug.getLaddrStart().getIpv4Address() != null) {
                    request.entry.localAddressStart = ipv4AddressToAddress(aug.getLaddrStart().getIpv4Address());
                } else if (aug.getLaddrStart().getIpv6Address() != null) {
                    request.entry.localAddressStart = ipv6AddressToAddress(aug.getLaddrStart().getIpv6Address());
                }
            }

            if (aug.getLaddrStop() != null) {
                if (aug.getLaddrStop().getIpv4Address() != null) {
                    request.entry.localAddressStop = ipv4AddressToAddress(aug.getLaddrStop().getIpv4Address());
                } else if (aug.getLaddrStop().getIpv6Address() != null) {
                    request.entry.localAddressStop = ipv6AddressToAddress(aug.getLaddrStop().getIpv6Address());
                }
            }

            if (aug.getRaddrStop() != null) {
                if (aug.getRaddrStop().getIpv4Address() != null) {
                    request.entry.remoteAddressStop = ipv4AddressToAddress(aug.getRaddrStop().getIpv4Address());
                } else if (aug.getRaddrStop().getIpv6Address() != null) {
                    request.entry.remoteAddressStop = ipv6AddressToAddress(aug.getRaddrStop().getIpv6Address());
                }
            }

            if (aug.getRaddrStart() != null) {
                if (aug.getRaddrStart().getIpv4Address() != null) {
                    request.entry.remoteAddressStart = ipv4AddressToAddress(aug.getRaddrStart().getIpv4Address());
                } else if (aug.getRaddrStart().getIpv6Address() != null) {
                    request.entry.remoteAddressStart = ipv6AddressToAddress(aug.getRaddrStart().getIpv6Address());
                }
            }
        }
        return request;
    }
}
