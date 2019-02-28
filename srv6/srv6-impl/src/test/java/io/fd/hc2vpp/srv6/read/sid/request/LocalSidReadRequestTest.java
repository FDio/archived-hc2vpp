/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.srv6.read.sid.request;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.srv6.write.sid.request.LocalSidRequestTest;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.jvpp.core.dto.SrLocalsidsDetails;
import io.fd.jvpp.core.dto.SrLocalsidsDetailsReplyDump;
import io.fd.jvpp.core.types.Srv6Sid;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.Locator1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.routing.srv6.locators.locator.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.routing.srv6.locators.locator._static.LocalSids;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.Sid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.SidBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.SidKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.Routing1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.routing.Srv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.Locators;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.Locator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.LocatorKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndT;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndX;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.Srv6FuncOpcodeUnreserved;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LocalSidReadRequestTest extends LocalSidRequestTest {

    private static final String LOCAL_0 = "local0";
    private static final String VLAN_0 = "vlan0";
    private static final Ipv6Address SID_ADR = new Ipv6Address("a::100");
    private static final Ipv6Address SID_ADR_2 = new Ipv6Address("a::101");
    private static final Ipv6AddressNoZone ADDRESS_NO_ZONE = new Ipv6AddressNoZone(SID_ADR_2);
    private static final long OPERATION = 256L; // 100 in hex IPv6 format
    private static final InstanceIdentifier<Sid> ID_SID =
            InstanceIdentifier.create(Routing.class)
                    .augmentation(Routing1.class)
                    .child(Srv6.class)
                    .child(Locators.class)
                    .child(Locator.class, new LocatorKey("a::"))
                    .augmentation(Locator1.class)
                    .child(Static.class)
                    .child(LocalSids.class)
                    .child(Sid.class, new SidKey(new Srv6FuncOpcodeUnreserved(OPERATION)));

    @Mock
    private ReadContext readCtx;

    @Mock
    private CompletionStage<SrLocalsidsDetailsReplyDump> stage;

    @Mock
    private CompletableFuture<SrLocalsidsDetailsReplyDump> detailsFuture;

    @Mock
    private ModificationCache modificationCache;

    @Mock
    private MappingContext mappingContext;

    private SrLocalsidsDetailsReplyDump replyDump = new SrLocalsidsDetailsReplyDump();

    @Override
    protected void init() {
        MockitoAnnotations.initMocks(this);
        defineMapping(mappingContext, LOCAL_0, 1, "interface-context");
        defineMapping(mappingContext, VLAN_0, 2, "interface-context");
        replyDump.srLocalsidsDetails = new ArrayList<>();
        when(ctx.getMappingContext()).thenReturn(mappingContext);
        when(readCtx.getMappingContext()).thenReturn(mappingContext);
        when(readCtx.getModificationCache()).thenReturn(modificationCache);
        when(modificationCache.get(any())).thenReturn(replyDump);
        when(api.srLocalsidsDump(any())).thenReturn(stage);
        when(stage.toCompletableFuture()).thenReturn(detailsFuture);
        when(locatorContext.getLocator(eq(LOCATOR.getName()), any())).thenReturn(new Ipv6Prefix("a::/64"));


        try {
            when(detailsFuture.get()).thenReturn(replyDump);
        } catch (InterruptedException | ExecutionException e) {
            // noop
        }
    }

    @Test
    public void readAllKeysTest() throws ReadFailedException {
        SrLocalsidsDetails srLocalsidsDetails = new SrLocalsidsDetails();
        Srv6Sid sid = new Srv6Sid();
        sid.addr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(SID_ADR));
        srLocalsidsDetails.addr = sid;

        SrLocalsidsDetails srLocalsidsDetails2 = new SrLocalsidsDetails();
        Srv6Sid sid2 = new Srv6Sid();
        sid2.addr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(SID_ADR_2));
        srLocalsidsDetails2.addr = sid2;

        replyDump.srLocalsidsDetails.add(srLocalsidsDetails);
        replyDump.srLocalsidsDetails.add(srLocalsidsDetails2);

        final LocalSidReadRequest request = new LocalSidReadRequest(api, locatorContext, READ_REGISTRY);
        request.checkValid();
        List<SidKey> sidKeys = request.readAllKeys(ID_SID, readCtx);

        Assert.assertFalse(sidKeys.isEmpty());
        Assert.assertEquals(2, sidKeys.size());
    }

    @Test
    public void readSpecificEndXTest() throws ReadFailedException {
        SrLocalsidsDetails endX = new SrLocalsidsDetails();
        endX.behavior = 2;
        endX.endPsp = 0;
        endX.xconnectNhAddr6 = AddressTranslator.INSTANCE.ipv6AddressNoZoneToArray(SID_ADR_2);
        endX.xconnectIfaceOrVrfTable = 1;

        Srv6Sid sid = new Srv6Sid();
        sid.addr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(SID_ADR));
        endX.addr = sid;

        replyDump.srLocalsidsDetails.add(endX);

        final LocalSidReadRequest request = new LocalSidReadRequest(api, locatorContext, READ_REGISTRY);
        SidBuilder sidBuilder = new SidBuilder();
        request.readSpecific(ID_SID, readCtx, sidBuilder);

        Assert.assertNotNull(sidBuilder.getEndX());
        Assert.assertEquals(EndX.class, sidBuilder.getEndBehaviorType());
        Assert.assertEquals(OPERATION, sidBuilder.getOpcode().getValue().longValue());
        Assert.assertNotNull(sidBuilder.getEndX().getPaths().getPath());
        Assert.assertFalse(sidBuilder.getEndX().getPaths().getPath().isEmpty());
        Assert.assertEquals(LOCAL_0, sidBuilder.getEndX().getPaths().getPath().get(0).getInterface());
        Assert.assertEquals(ADDRESS_NO_ZONE, sidBuilder.getEndX().getPaths().getPath().get(0).getNextHop());
    }


    @Test
    public void readSpecificEndTTest() throws ReadFailedException {
        SrLocalsidsDetails endT = new SrLocalsidsDetails();
        endT.behavior = 3;
        endT.xconnectIfaceOrVrfTable = 4;
        endT.endPsp = 0;

        Srv6Sid sid = new Srv6Sid();
        sid.addr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(SID_ADR));
        endT.addr = sid;

        replyDump.srLocalsidsDetails.add(endT);

        final LocalSidReadRequest request = new LocalSidReadRequest(api, locatorContext, READ_REGISTRY);
        SidBuilder sidBuilder = new SidBuilder();
        request.readSpecific(ID_SID, readCtx, sidBuilder);

        Assert.assertNotNull(sidBuilder.getEndT());
        Assert.assertEquals(EndT.class, sidBuilder.getEndBehaviorType());
        Assert.assertEquals(OPERATION, sidBuilder.getOpcode().getValue().longValue());
        Assert.assertEquals(4L, sidBuilder.getEndT().getLookupTableIpv6().getValue().longValue());
    }
}
