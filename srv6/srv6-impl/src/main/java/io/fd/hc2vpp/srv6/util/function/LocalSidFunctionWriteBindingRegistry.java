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
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.Sid;

public class LocalSidFunctionWriteBindingRegistry<T extends LocalSidFunctionRequest>
        extends LocalSidFunctionBindingRegistry<T> {

    public LocalSidFunctionRequest bind(final Sid localSid, @Nonnull final WriteContext ctx) {
        return binders.parallelStream()
                .filter(toLocalSidFunctionBinder -> toLocalSidFunctionBinder.canHandle(localSid))
                .map(binder -> binder.createWriteRequestAndBind(localSid, ctx))
                .collect(RWUtils.singleItemCollector());
    }
}
