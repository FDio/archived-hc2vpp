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

package io.fd.hc2vpp.docs.core.mock.binding;

import static io.fd.hc2vpp.docs.core.mock.binding.MockBindingModule.noOpProxy;

import com.google.inject.Provider;
import io.fd.hc2vpp.nat.NatModule;
import io.fd.vpp.jvpp.JVpp;
import io.fd.vpp.jvpp.JVppRegistry;
import io.fd.vpp.jvpp.nat.future.FutureJVppNatFacade;
import java.io.IOException;

/**
 * Use to bypass jvpp registration
 */
public class MockNatModule extends NatModule {

    public MockNatModule() {
        super(MockJVppNatProvider.class);
    }

    private static class MockJVppNatProvider implements Provider<FutureJVppNatFacade> {

        @Override
        public FutureJVppNatFacade get() {
            try {
                return new FutureJVppNatFacade(noOpProxy(JVppRegistry.class), noOpProxy(JVpp.class));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
