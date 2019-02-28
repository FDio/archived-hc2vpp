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

package io.fd.hc2vpp.srv6.util.function;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.jvpp.core.dto.SrLocalsidsDetails;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.SidBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.Srv6EndpointType;

/**
 * Binder interface, which is used to map local sid function requests from VPP to yang model data classes of
 * local sid functions. It uses behavior function type integer value defined by VPP API, to find
 * suitable binder. This value is translated to {@link Srv6EndpointType} in model, which represents the same endpoint
 * function as defined by VPP API.
 */
public interface LocalSidFunctionReadBinder {

    /**
     * Translate data read From VPP to data defined by model
     *
     * @param data local sid details read from VPP
     * @param ctx read context that contains modification cache and mapping chache
     * @param builder builder for setting data
     */
    void translateFromDump(@Nonnull final SrLocalsidsDetails data, @Nonnull final ReadContext ctx,
                           @Nonnull final SidBuilder builder);

    /**
     * Provide behavior function type integer value.
     *
     * @return integer value of behaviour function type as defined in VPP api
     */
    int getBehaviourFunctionType();

    /**
     * Checks whether this binder is able to process provided function from VPP
     *
     * @param functionCode integer value of behaviour function type as defined in VPP api
     * @return true if endpoint function binder is able to process provided functionCode, false otherwise
     */
    default boolean canHandle(int functionCode) {
        return getBehaviourFunctionType() == functionCode;
    }
}
