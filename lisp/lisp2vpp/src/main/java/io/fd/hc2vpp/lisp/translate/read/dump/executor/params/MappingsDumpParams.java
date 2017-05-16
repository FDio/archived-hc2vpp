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

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Map;

/**
 * Parameters for executing dump of mappings
 */
public final class MappingsDumpParams {

    private final byte eidSet;
    private final byte prefixLength;
    private final int vni;
    private final byte eidType;
    private final byte[] eid;
    private final byte filter;

    private MappingsDumpParams(MappingsDumpParamsBuilder builder) {
        this.eidSet = builder.eidSet;
        this.prefixLength = builder.prefixLength;
        this.vni = builder.vni;
        this.eidType = builder.eidType;
        this.eid = builder.eid;
        this.filter = builder.filter;
    }


    public byte getEidSet() {
        return eidSet;
    }

    public byte getPrefixLength() {
        return prefixLength;
    }

    public int getVni() {
        return vni;
    }

    public byte getEidType() {
        return eidType;
    }

    public byte[] getEid() {
        return eid;
    }

    public final byte getFilter() {
        return filter;
    }

    @Override
    public String toString() {
        return "MappingsDumpParams{" +
                "eidSet=" + eidSet +
                ", prefixLength=" + prefixLength +
                ", vni=" + vni +
                ", eidType=" + eidType +
                ", eid=" + Arrays.toString(eid) +
                ", filter=" + filter +
                '}';
    }

    /**
     * Type of requested mapping eid
     */
    public enum EidType {
        IPV4(0),
        IPV6(1),
        MAC(2),
        IPV4_PREFIX(3),
        IPV6_PREFIX(4);

        /**
         * From vpp api perspective, ipv4 is the same as ipv4-prefix, it differs just in prefix value.
         * Respectively for ipv6/ipv6-prefix
         */
        private static final Map<EidType, Integer> VPP_EID_TYPE_REGISTER = ImmutableMap.of(
                IPV4, 0, IPV6, 1, MAC, 2, IPV4_PREFIX, 0, IPV6_PREFIX, 1);

        /**
         * This value should be use just for our logic, do not use it to bind vpp requests
         */
        private final int value;

        private EidType(final int value) {
            this.value = value;
        }

        public static EidType valueOf(int value) {
            switch (value) {
                case 0:
                    return IPV4;
                case 1:
                    return IPV6;
                case 2:
                    return MAC;
                case 3:
                    return IPV4_PREFIX;
                case 4:
                    return IPV6_PREFIX;
                default:
                    throw new IllegalArgumentException("Illegal value");
            }
        }

        public final int getVppTypeBinding() {
            return VPP_EID_TYPE_REGISTER.get(this);
        }
    }

    /**
     * Type of requested mapping
     */
    public enum FilterType {
        ALL(0),
        LOCAL(1),
        REMOTE(2);

        private final int value;

        private FilterType(final int value) {
            this.value = value;
        }

        public final int getValue() {
            return this.value;
        }
    }

    public enum QuantityType {
        ALL(0),
        SPECIFIC(1);

        private final int value;

        private QuantityType(final int value) {
            this.value = value;
        }

        public final int getValue() {
            return this.value;
        }
    }

    public static final class MappingsDumpParamsBuilder {
        private byte eidSet;
        private byte prefixLength;
        private int vni;
        private byte eidType;
        private byte[] eid;
        private byte filter;

        public MappingsDumpParamsBuilder setEidSet(final QuantityType quantityType) {
            this.eidSet = (byte) quantityType.getValue();
            return this;
        }

        public MappingsDumpParamsBuilder setPrefixLength(final byte prefixLength) {
            this.prefixLength = prefixLength;
            return this;
        }

        public MappingsDumpParamsBuilder setVni(final int vni) {
            this.vni = vni;
            return this;
        }

        public MappingsDumpParamsBuilder setEidType(final EidType eidType) {
            this.eidType = (byte) eidType.getVppTypeBinding();
            return this;
        }

        public MappingsDumpParamsBuilder setEid(final byte[] eid) {
            this.eid = eid;
            return this;
        }

        public MappingsDumpParamsBuilder setFilter(final FilterType filterType) {
            this.filter = (byte) filterType.getValue();
            return this;
        }

        public MappingsDumpParams build() {
            return new MappingsDumpParams(this);
        }
    }
}
