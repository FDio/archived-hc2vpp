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

package io.fd.honeycomb.v3po.vpp.facade.impl.read.util;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Might be slow !
 */
public class ReflexiveRootReaderCustomizer<C extends DataObject, B extends Builder<C>>  extends NoopReaderCustomizer<C, B> {

    private final Class<B> builderClass;

    public ReflexiveRootReaderCustomizer(final Class<B> builderClass) {
        this.builderClass = builderClass;
    }

    @Override
    public B getBuilder(InstanceIdentifier<C> id) {
        try {
            return builderClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to instantiate " + builderClass, e);
        }
    }
}
