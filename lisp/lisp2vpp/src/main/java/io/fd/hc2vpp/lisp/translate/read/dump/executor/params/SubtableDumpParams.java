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

package io.fd.hc2vpp.lisp.translate.read.dump.executor.params;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.VrfSubtable;

/**
 * Parameters for dumping {@link VrfSubtable}/{@link BridgeDomainSubtable}
 */
public final class SubtableDumpParams {

    private final byte isL2;

    private SubtableDumpParams(SubtableDumpParamsBuilder builder) {
        this.isL2 = builder.isL2;
    }

    public byte isL2() {
        return isL2;
    }

    @Override
    public String toString() {
        return "SubtableDumpParams{" +
                "isL2=" + isL2 +
                '}';
    }

    public enum MapLevel {
        L2(1),
        L3(0);

        private final int value;

        MapLevel(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static final class SubtableDumpParamsBuilder implements ByteDataTranslator {

        private byte isL2;

        public SubtableDumpParamsBuilder setL2(@Nonnull final MapLevel mapLevel) {
            isL2 = booleanToByte(MapLevel.L2.equals(mapLevel));
            return this;
        }

        public SubtableDumpParams build() {
            return new SubtableDumpParams(this);
        }
    }
}
