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

package io.fd.hc2vpp.v3po.interfacesstate.cache;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.inject.Named;

public class InterfaceCacheDumpManagerProvider implements Provider<InterfaceCacheDumpManager> {

    @Inject
    private FutureJVppCore jvpp;

    @Inject
    @Named("interface-context")
    private NamingContext namingContext;

    @Override
    public InterfaceCacheDumpManager get() {
        return new InterfaceCacheDumpManagerImpl(jvpp, namingContext);
    }
}
