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

package io.fd.hc2vpp.common.translate.util;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.jvpp.core.types.FibMplsLabel;
import javax.annotation.Nonnull;

/**
 * Utility for Translating between different representations of MPLS label.
 */
public interface MplsLabelTranslator {
    /**
     * Make available also from static context.
     */
    MplsLabelTranslator INSTANCE = new MplsLabelTranslator() {
    };

    /**
     * Builds {@link FibMplsLabel} from its YANG representation.
     *
     * @param label YANG representation of MPLS Label
     * @return VPP representation of MPLS label
     */
    default FibMplsLabel translate(@Nonnull final Long label) {
        checkNotNull(label, "MPLS label should not be null");
        return translate(label.intValue());
    }

    /**
     * Builds {@link FibMplsLabel} from int value.
     *
     * @param label MPLS Label value
     * @return VPP representation of MPLS label
     */
    default FibMplsLabel translate(final int label) {
        final FibMplsLabel fibMplsLabel = new FibMplsLabel();
        fibMplsLabel.label = label;
        return fibMplsLabel;
    }
}
