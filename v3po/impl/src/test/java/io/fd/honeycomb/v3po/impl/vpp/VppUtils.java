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

import io.fd.honeycomb.v3po.impl.trans.util.VppRWUtils;
import io.fd.honeycomb.v3po.impl.trans.w.ChildVppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.impl.CompositeChildVppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.impl.CompositeListVppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.impl.CompositeRootVppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.util.NoopWriterCustomizer;
import io.fd.honeycomb.v3po.impl.trans.w.util.ReflexiveChildWriterCustomizer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.openvpp.vppjapi.vppApi;

final class VppUtils {

    public VppUtils() {}

    /**
     * Create root Vpp writer with all its children wired
     */
    static CompositeRootVppWriter<Vpp> getVppWriter(@Nonnull final vppApi vppApi) {

        final CompositeListVppWriter<BridgeDomain, BridgeDomainKey> bridgeDomainWriter = new CompositeListVppWriter<>(
            BridgeDomain.class,
            new io.fd.honeycomb.v3po.impl.vpp.BridgeDomainCustomizer(vppApi));

        final ChildVppWriter<BridgeDomains> bridgeDomainsReader = new CompositeChildVppWriter<>(
            BridgeDomains.class,
            VppRWUtils.singletonChildWriterList(bridgeDomainWriter),
            new ReflexiveChildWriterCustomizer<BridgeDomains>());

        final List<ChildVppWriter<? extends ChildOf<Vpp>>> childWriters = new ArrayList<>();
        childWriters.add(bridgeDomainsReader);

        return new CompositeRootVppWriter<>(
            Vpp.class,
            childWriters,
            new NoopWriterCustomizer<Vpp>());
    }
}
