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
import io.fd.hc2vpp.vppioam.impl.VppIoamModule;
import io.fd.jvpp.JVpp;
import io.fd.jvpp.JVppRegistry;
import io.fd.jvpp.ioamexport.future.FutureJVppIoamexportFacade;
import io.fd.jvpp.ioampot.future.FutureJVppIoampotFacade;
import io.fd.jvpp.ioamtrace.future.FutureJVppIoamtraceFacade;
import java.io.IOException;

/**
 * Use to bypass jvpp registration
 */
public class MockIoamModule extends VppIoamModule {

    public MockIoamModule() {
        super(MockTraceProvider.class, MockPotProvider.class, MockExportProvider.class);
    }

    private static class MockTraceProvider implements Provider<FutureJVppIoamtraceFacade> {
        @Override
        public FutureJVppIoamtraceFacade get() {
            try {
                return new FutureJVppIoamtraceFacade(noOpProxy(JVppRegistry.class), noOpProxy(JVpp.class));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static class MockPotProvider implements Provider<FutureJVppIoampotFacade> {

        @Override
        public FutureJVppIoampotFacade get() {
            try {
                return new FutureJVppIoampotFacade(noOpProxy(JVppRegistry.class), noOpProxy(JVpp.class));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static class MockExportProvider implements Provider<FutureJVppIoamexportFacade> {

        @Override
        public FutureJVppIoamexportFacade get() {
            try {
                return new FutureJVppIoamexportFacade(noOpProxy(JVppRegistry.class), noOpProxy(JVpp.class));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
