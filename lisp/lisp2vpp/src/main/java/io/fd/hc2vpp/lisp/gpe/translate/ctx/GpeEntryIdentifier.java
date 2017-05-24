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

import io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams;
import io.fd.hc2vpp.lisp.translate.util.EidTranslator;
import io.fd.vpp.jvpp.core.types.GpeFwdEntry;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.mapping.GpeEntryIdentificator;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.mapping.gpe.entry.identificator.LocalEid;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.mapping.gpe.entry.identificator.LocalEidBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.mapping.gpe.entry.identificator.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.mapping.gpe.entry.identificator.RemoteEidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;


/**
 * Uniquely identifies gpe entry in mapping context
 */
public final class GpeEntryIdentifier implements EidTranslator {

    private final long vni;
    private final LocalEid localEid;
    private final RemoteEid remoteEid;

    private GpeEntryIdentifier(final long vni,
                               @Nonnull  final LocalEid localEid,
                               @Nonnull  final RemoteEid remoteEid) {
        this.vni = vni;
        this.localEid = localEid;
        this.remoteEid = remoteEid;
    }

    public long getVni() {
        return vni;
    }

    public LocalEid getLocalEid() {
        return localEid;
    }

    public RemoteEid getRemoteEid() {
        return remoteEid;
    }

    public boolean isSame(@Nonnull final GpeEntryIdentificator identificator) {
        return new EqualsBuilder()
                .append(true, compareEids(this.getLocalEid(), identificator.getLocalEid()))
                .append(true, compareEids(this.getRemoteEid(), identificator.getRemoteEid()))
                .append(this.vni, identificator.getVni().longValue())
                .isEquals();
    }

    public static GpeEntryIdentifier fromEntry(final GpeEntry data) {
        return new GpeEntryIdentifier.GpeEntryIdentifierBuilder()
                .setLocalEid(data.getLocalEid())
                .setRemoteEid(data.getRemoteEid())
                .setVni(data.getVni())
                .createGpeEntryIdentifier();
    }

    public static GpeEntryIdentifier fromDumpDetail(final GpeFwdEntry entry) {
        return new GpeEntryIdentifier.GpeEntryIdentifierBuilder()
                .setVni(entry.vni)
                .setLocalEid(
                        INSTANCE.getArrayAsGpeLocalEid(MappingsDumpParams.EidType.valueOf(entry.eidType), entry.leid,
                                entry.leidPrefixLen, entry.vni))
                .setRemoteEid(
                        INSTANCE.getArrayAsGpeRemoteEid(MappingsDumpParams.EidType.valueOf(entry.eidType), entry.reid,
                                entry.reidPrefixLen, entry.vni))
                .createGpeEntryIdentifier();
    }

    public static final class GpeEntryIdentifierBuilder {
        private long vni;
        private LocalEid localEid;
        private RemoteEid remoteEid;

        public GpeEntryIdentifierBuilder setVni(final long vni) {
            this.vni = vni;
            return this;
        }

        public GpeEntryIdentifierBuilder setLocalEid(
                @Nonnull final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocalEid localEid) {
            this.localEid = new LocalEidBuilder()
                    .setAddress(localEid.getAddress())
                    .setAddressType(localEid.getAddressType())
                    .setVirtualNetworkId(localEid.getVirtualNetworkId())
                    .build();
            return this;
        }

        public GpeEntryIdentifierBuilder setRemoteEid(
                @Nonnull final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEid remoteEid) {
            this.remoteEid = new RemoteEidBuilder()
                    .setAddress(remoteEid.getAddress())
                    .setAddressType(remoteEid.getAddressType())
                    .setVirtualNetworkId(remoteEid.getVirtualNetworkId())
                    .build();
            return this;
        }

        public GpeEntryIdentifier createGpeEntryIdentifier() {
            return new GpeEntryIdentifier(vni, localEid, remoteEid);
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

        final GpeEntryIdentifier that = (GpeEntryIdentifier) o;

        if (vni != that.vni) {
            return false;
        }
        if (!localEid.equals(that.localEid)) {
            return false;
        }
        return remoteEid.equals(that.remoteEid);
    }

    @Override
    public int hashCode() {
        int result = (int) (vni ^ (vni >>> 32));
        result = 31 * result + localEid.hashCode();
        result = 31 * result + remoteEid.hashCode();
        return result;
    }
}
