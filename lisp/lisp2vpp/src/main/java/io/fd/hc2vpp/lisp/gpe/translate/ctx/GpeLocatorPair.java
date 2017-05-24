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

package io.fd.hc2vpp.lisp.gpe.translate.ctx;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.vpp.jvpp.core.dto.GpeFwdEntryPathDetails;
import java.util.Arrays;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.mapping.locator.pair.mapping.Pair;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocatorPairs;

public final class GpeLocatorPair implements AddressTranslator {

    private final IpAddress localAddress;
    private final IpAddress remoteAddress;

    private GpeLocatorPair(@Nonnull final IpAddress localAddress, @Nonnull  final IpAddress remoteAddress) {
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public IpAddress getLocalAddress() {
        return localAddress;
    }

    public IpAddress getRemoteAddress() {
        return remoteAddress;
    }

    public boolean isSame(@Nonnull final Pair pair) {
        return new EqualsBuilder()
                .append(true, Arrays.equals(this.localAddress.getValue(), pair.getLocalAddress().getValue()))
                .append(true, Arrays.equals(this.remoteAddress.getValue(), pair.getRemoteAddress().getValue()))
                .isEquals();
    }

    public static GpeLocatorPair fromDumpDetail(final GpeFwdEntryPathDetails entry) {
        return new GpeLocatorPair.GpeLocatorPairBuilder()
                .setLocalAddress(
                        INSTANCE.arrayToIpAddress(!INSTANCE.byteToBoolean(entry.lclLoc.isIp4),
                                entry.lclLoc.addr))
                .setRemoteAddress(
                        INSTANCE.arrayToIpAddress(!INSTANCE.byteToBoolean(entry.rmtLoc.isIp4),
                                entry.rmtLoc.addr))
                .createGpeLocatorPairIdentifier();
    }

    public static GpeLocatorPair fromLocatorPair(final LocatorPairs locatorPair) {
        return new GpeLocatorPair.GpeLocatorPairBuilder()
                .setLocalAddress(locatorPair.getLocatorPair().getLocalLocator())
                .setRemoteAddress(locatorPair.getLocatorPair().getRemoteLocator())
                .createGpeLocatorPairIdentifier();
    }

    public static final class GpeLocatorPairBuilder {
        private IpAddress localAddress;
        private IpAddress remoteAddress;

        public GpeLocatorPairBuilder setLocalAddress(@Nonnull final IpAddress localAddress) {
            this.localAddress = localAddress;
            return this;
        }

        public GpeLocatorPairBuilder setRemoteAddress(@Nonnull final IpAddress remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }

        public GpeLocatorPair createGpeLocatorPairIdentifier() {
            return new GpeLocatorPair(localAddress, remoteAddress);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final GpeLocatorPair that = (GpeLocatorPair) o;

        if (!localAddress.equals(that.localAddress)) {
            return false;
        }
        return remoteAddress.equals(that.remoteAddress);
    }

    @Override
    public int hashCode() {
        int result = localAddress.hashCode();
        result = 31 * result + remoteAddress.hashCode();
        return result;
    }
}
