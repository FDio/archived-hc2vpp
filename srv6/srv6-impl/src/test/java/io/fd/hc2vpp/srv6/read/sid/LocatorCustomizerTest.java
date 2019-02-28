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

package io.fd.hc2vpp.srv6.read.sid;

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
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.LocatorsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.Locator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.LocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.LocatorKey;

public class LocatorCustomizerTest extends LocalSidRequestTest {

    private static final Ipv6Address SID_ADR = new Ipv6Address("a::101");

    @Mock
    private ReadContext readCtx;

    @Mock
    private ModificationCache modificationCache;

    private SrLocalsidsDetailsReplyDump replyDump = new SrLocalsidsDetailsReplyDump();

    @Override
    protected void init() {
        when(readCtx.getModificationCache()).thenReturn(modificationCache);
        when(modificationCache.get(any())).thenReturn(replyDump);
        when(locatorContext.getLocator(eq(LOCATOR.getName()), any())).thenReturn(new Ipv6Prefix("a::/64"));
    }

    @Test
    public void getAllIdsTest() throws ReadFailedException {
        SrLocalsidsDetails srLocalsidsDetails = new SrLocalsidsDetails();
        srLocalsidsDetails.behavior = 1;
        Srv6Sid sid = new Srv6Sid();
        sid.addr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(SID_ADR));
        srLocalsidsDetails.addr = sid;
        replyDump.srLocalsidsDetails.add(srLocalsidsDetails);

        LocatorCustomizer customizer = new LocatorCustomizer(api, locatorContext);
        List<LocatorKey> allIds = customizer.getAllIds(SID_A_101.firstIdentifierOf(Locator.class), readCtx);

        Assert.assertNotNull(allIds);
        Assert.assertFalse(allIds.isEmpty());
        Assert.assertTrue(allIds.contains(SID_A_101.firstKeyOf(Locator.class)));
    }

    @Test
    public void readCurrentAttributesTest() throws ReadFailedException {
        SrLocalsidsDetails srLocalsidsDetails = new SrLocalsidsDetails();
        srLocalsidsDetails.behavior = 1;
        Srv6Sid sid = new Srv6Sid();
        sid.addr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(SID_ADR));
        srLocalsidsDetails.addr = sid;
        replyDump.srLocalsidsDetails.add(srLocalsidsDetails);

        LocatorCustomizer customizer = new LocatorCustomizer(api, locatorContext);
        LocatorBuilder builder = new LocatorBuilder();
        customizer.readCurrentAttributes(SID_A_101.firstIdentifierOf(Locator.class), builder, readCtx);
        Assert.assertEquals(SID_A_101.firstKeyOf(Locator.class), builder.key());
        Assert.assertNotNull(customizer.getBuilder(SID_A_101.firstIdentifierOf(Locator.class)));

        LocatorsBuilder parentBuilder = new LocatorsBuilder();
        customizer.merge(parentBuilder, Collections.singletonList(builder.build()));

        Assert.assertNotNull(parentBuilder.getLocator());
        Assert.assertFalse(parentBuilder.getLocator().isEmpty());
        Assert.assertTrue(parentBuilder.getLocator().contains(builder.build()));
    }
}
