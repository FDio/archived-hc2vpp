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

package io.fd.honeycomb.vpp.test.util;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import javax.annotation.Nonnull;
import org.openvpp.jvpp.core.dto.SwInterfaceDetails;
import org.openvpp.jvpp.core.dto.SwInterfaceDetailsReplyDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;

/**
 * VPP translation test helper, that helps stubbing {@link FutureJVppCore#swInterfaceDump}.
 */
public interface InterfaceDumpHelper extends FutureProducer {

    /**
     * Stubs swInterfaceDump to return given list of interfaces.
     *
     * @param api        vppApi reference
     * @param interfaces list of interface details to be returned
     */
    default void whenSwInterfaceDumpThenReturn(@Nonnull final FutureJVppCore api,
                                               final SwInterfaceDetails... interfaces) {
        final SwInterfaceDetailsReplyDump reply = new SwInterfaceDetailsReplyDump();
        Collections.addAll(reply.swInterfaceDetails, interfaces);
        when(api.swInterfaceDump(any())).thenReturn(future(reply));
    }
}
