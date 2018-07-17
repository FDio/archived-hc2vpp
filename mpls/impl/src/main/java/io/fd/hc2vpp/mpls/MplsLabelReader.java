/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.mpls;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.EntropyLabelIndicator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.ExtensionLabel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.GalLabel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.ImplicitNullLabel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.Ipv4ExplicitNullLabel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.Ipv6ExplicitNullLabel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.MplsLabelSpecialPurposeValue;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.OamAlertLabel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.RouterAlertLabel;

/**
 * Mixin that reads integer value of {@link MplsLabel}.
 */
interface MplsLabelReader {

    default int getLabelValue(final MplsLabel label) {
        if (label.getMplsLabelGeneralUse() != null) {
            return label.getMplsLabelGeneralUse().getValue().intValue();
        } else if (label.getMplsLabelSpecialPurpose() != null ) {
            final Class<? extends MplsLabelSpecialPurposeValue> specialLabel =
                label.getMplsLabelSpecialPurpose();
            // Encoding of labels 0-3
            // https://tools.ietf.org/html/rfc3032#section-2.1
            if (Ipv4ExplicitNullLabel.class.equals(specialLabel)) {
                return 0;
            } else if (RouterAlertLabel.class.equals(specialLabel)) {
                return 1;
            } else if (Ipv6ExplicitNullLabel.class.equals(specialLabel)) {
                return 2;
            } else if (ImplicitNullLabel.class.equals(specialLabel)) {
                return 3;
            } else if (EntropyLabelIndicator.class.equals(specialLabel)) {
                // https://tools.ietf.org/html/rfc6790#section-3
                return 7;
            } else if (GalLabel.class.equals(specialLabel)) {
                // https://tools.ietf.org/html/rfc5586#section-4
                return 13;
            } else if (OamAlertLabel.class.equals(specialLabel)) {
                // https://tools.ietf.org/html/rfc3429#section-3
                return 14;
            } else if (ExtensionLabel.class.equals(specialLabel)) {
                // https://tools.ietf.org/html/rfc7274#section-3.1
                return 15;
            } else {
                throw new IllegalArgumentException("Unsupported special purpose MPLS label: " + specialLabel);
            }
        } else {
            throw new IllegalArgumentException("Unsupported MPLS label: " + label);
        }
    }
}
