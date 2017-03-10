/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.l3.utils.ip.read;

import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.util.read.cache.CacheKeyFactory;
import io.fd.honeycomb.translate.util.read.cache.TypeAwareIdentifierCacheKeyFactory;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces.state._interface.sub.interfaces.SubInterface;

/**
 * Utility class providing Ipv4/6 read support.
 */
public abstract class IpReader implements AddressTranslator, JvppReplyConsumer {

    private final NamingContext interfaceContext;
    private final boolean isIpv6;

    IpReader(@Nonnull final NamingContext interfaceContext, final boolean isIpv6) {
        this.interfaceContext = interfaceContext;
        this.isIpv6 = isIpv6;
    }

    @Nonnull
    protected static CacheKeyFactory interfaceScopedCacheKeyFactory(@Nonnull final Class<?> dumpReplyClass) {
        return new TypeAwareIdentifierCacheKeyFactory(dumpReplyClass, ImmutableSet.of(Interface.class));
    }

    @Nonnull
    protected static CacheKeyFactory subInterfaceScopedCacheKeyFactory(@Nonnull final Class<?> dumpReplyClass) {
        return new TypeAwareIdentifierCacheKeyFactory(dumpReplyClass, ImmutableSet.of(SubInterface.class));
    }

    protected NamingContext getInterfaceContext() {
        return interfaceContext;
    }

    protected boolean isIpv6() {
        return isIpv6;
    }
}
