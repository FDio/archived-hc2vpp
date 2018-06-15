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

import io.fd.hc2vpp.srv6.write.sid.request.LocalSidFunctionRequest;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.Sid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.Srv6EndpointType;

/**
 * Binder interface, which is used to map yang model data classes of local sid functions to local sid function requests
 * used to configure endpoint functions on VPP. It uses {@link Srv6EndpointType} class value, to find suitable binder.
 * This value is translated to behavior function type integer value defined by VPP API, which represents the same
 * function as defined by the model.
 *
 * @param <T> Type which extends general interface for {@link LocalSidFunctionRequest} and represents template binder
 *            that is used to process end function data represented by provided class type.
 */
public interface LocalSidFunctionWriteBinder<T extends LocalSidFunctionRequest> {

    /**
     * Binds request accordingly to type of function implemented by this interface
     *
     * @return request with all attributes necessary for this function
     */
    @Nonnull
    T createWriteRequestAndBind(@Nonnull final Sid data, @Nonnull final WriteContext ctx);

    /**
     * Provides Endpoint function type class.
     *
     * @return Endpoint function class
     */
    @Nonnull
    Class<? extends Srv6EndpointType> getHandledFunctionType();

    /**
     * Checks whether binder can handle provided Sid data from model
     *
     * @param data sid function data to be checked
     * @return true if function binder is able to process provided data, false otherwise
     */
    default boolean canHandle(final Sid data) {
        if (data == null || data.getEndBehaviorType() == null) {
            return false;
        }
        return data.getEndBehaviorType().equals(getHandledFunctionType());
    }
}
