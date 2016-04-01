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

package io.fd.honeycomb.v3po.impl.vpp;

import static io.fd.honeycomb.v3po.impl.vpp.BridgeDomainTestUtils.BD_NAME_TO_ID_ANSWER;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.impl.trans.w.VppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.WriteContext;
import io.fd.honeycomb.v3po.impl.trans.w.impl.CompositeRootVppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.util.DelegatingWriterRegistry;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.vppjapi.vppApi;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.openvpp.vppjapi.vppConn")
@PrepareForTest(vppApi.class)
public class VppTest {

    private vppApi api;
    private DelegatingWriterRegistry rootRegistry;
    private CompositeRootVppWriter<Vpp> vppWriter;
    private WriteContext ctx;

    final byte zero = (byte) 0;
    final byte flood = (byte) 1;
    final byte forward = (byte) 0;
    final byte learn = (byte) 1;
    final byte uuf = (byte) 0;
    final byte arpTerm = (byte) 0;
    final byte add = (byte) 1;

    @Before
    public void setUp() throws Exception {
        api = PowerMockito.mock(vppApi.class);
        ctx = mock(WriteContext.class);
        PowerMockito.doAnswer(BD_NAME_TO_ID_ANSWER).when(api).findOrAddBridgeDomainId(anyString());
        PowerMockito.doAnswer(BD_NAME_TO_ID_ANSWER).when(api).bridgeDomainIdFromName(anyString());
        PowerMockito.doReturn(1).when(api).getRetval(anyInt(), anyInt());
        vppWriter = VppUtils.getVppWriter(api);
        rootRegistry = new DelegatingWriterRegistry(
            Collections.<VppWriter<? extends DataObject>>singletonList(vppWriter));
    }

    @Test
    public void writeVpp() throws Exception {
        rootRegistry.update(
            InstanceIdentifier.create(Vpp.class),
            Collections.<DataObject>emptyList(),
            Lists.newArrayList(new VppBuilder().setBridgeDomains(getBridgeDomains("bdn1")).build()),
            ctx);

        verify(api).bridgeDomainAddDel(1, flood, forward, learn, uuf, arpTerm, add);

        vppWriter.update(InstanceIdentifier.create(Vpp.class),
            Collections.<DataObject>emptyList(),
            Lists.newArrayList(new VppBuilder().setBridgeDomains(getBridgeDomains("bdn1")).build()),
            ctx);

        verify(api, times(2)).bridgeDomainAddDel(1, flood, forward, learn, uuf, arpTerm, add);
    }

    private BridgeDomains getBridgeDomains(String... name) {
        final List<BridgeDomain> bdmns = Lists.newArrayList();
        for (String s : name) {
            bdmns.add(new BridgeDomainBuilder()
                .setName(s)
                .setArpTermination(false)
                .setFlood(true)
                .setForward(false)
                .setLearn(true)
                .build());
        }
        return new BridgeDomainsBuilder()
                .setBridgeDomain(bdmns)
                .build();
    }

    @Test
    public void deleteVpp() throws Exception {
        rootRegistry.update(
            InstanceIdentifier.create(Vpp.class),
            Collections.singletonList(new VppBuilder().setBridgeDomains(getBridgeDomains("bdn1")).build()),
            Collections.<DataObject>emptyList(),
            ctx);

        final byte zero = (byte) 0;

        verify(api).bridgeDomainAddDel(1, zero, zero, zero, zero, zero, zero);
    }

    @Test
    public void updateVppNoActualChange() throws Exception {
        rootRegistry.update(
            InstanceIdentifier.create(Vpp.class),
            Collections.singletonList(new VppBuilder().setBridgeDomains(getBridgeDomains("bdn1")).build()),
            Collections.singletonList(new VppBuilder().setBridgeDomains(getBridgeDomains("bdn1")).build()),
            ctx);

        verifyZeroInteractions(api);
    }

    @Test
    public void writeBridgeDomain() throws Exception {
        rootRegistry.update(
            InstanceIdentifier.create(Vpp.class).child(BridgeDomains.class).child(BridgeDomain.class),
            getBridgeDomains("bdn1", "bdn2").getBridgeDomain(),
            getBridgeDomains("bdn1", "bdn3").getBridgeDomain(),
            ctx);

        // bdn1 is untouched
        // bdn3 is added
        verify(api).bridgeDomainAddDel(3, flood, forward, learn, uuf, arpTerm, add);
        // bdn2 is deleted
        verify(api).bridgeDomainAddDel(2, zero, zero, zero, zero, zero, zero);
    }

    // TODO test unkeyed list
    // TODO test update of a child without dedicated writer
}