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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.Routing1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.routing.Srv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.Locators;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.Locator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.LocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.LocatorKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LocatorReadRequestTest extends LocalSidRequestTest {

    private static final String LOCAL_0 = "local0";
    private static final Ipv6Address SID_ADR = new Ipv6Address("a::100");
    private static final Ipv6Address SID_ADR_2 = new Ipv6Address("b::101");
    private static final InstanceIdentifier<Locator> ID_LOC =
            InstanceIdentifier.create(Routing.class)
                    .augmentation(Routing1.class)
                    .child(Srv6.class)
                    .child(Locators.class)
                    .child(Locator.class, new LocatorKey("a::"));

    @Mock
    private ReadContext readCtx;

    @Mock
    private CompletionStage<SrLocalsidsDetailsReplyDump> stage;

    @Mock
    private CompletableFuture<SrLocalsidsDetailsReplyDump> detailsFuture;

    @Mock
    private ModificationCache modificationCache;

    private SrLocalsidsDetailsReplyDump replyDump = new SrLocalsidsDetailsReplyDump();

    @Override
    protected void init() {
        MockitoAnnotations.initMocks(this);
        defineMapping(mappingContext, LOCAL_0, 1, "interface-context");
        replyDump.srLocalsidsDetails = new ArrayList<>();
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

        final LocatorReadRequest request = new LocatorReadRequest(api, locatorContext);
        request.checkValid();
        List<LocatorKey> locatorKeys = request.readAllKeys(ID_LOC, readCtx);

        Assert.assertFalse(locatorKeys.isEmpty());
        Assert.assertEquals(2, locatorKeys.size());
        Assert.assertTrue(locatorKeys.contains(new LocatorKey("a::")));
        Assert.assertTrue(locatorKeys.contains(new LocatorKey("b::")));
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

        final LocatorReadRequest request = new LocatorReadRequest(api, locatorContext);
        LocatorBuilder builder = new LocatorBuilder();
        request.readSpecific(ID_LOC, readCtx, builder);

        Assert.assertEquals(new LocatorKey("a::"), builder.key());
    }
}
