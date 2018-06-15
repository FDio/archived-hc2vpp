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

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.srv6.write.sid.request.LocalSidFunctionRequest;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class LocalSidFunctionBindingRegistry<T extends LocalSidFunctionRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(LocalSidFunctionBindingRegistry.class);
    final List<LocalSidFunctionWriteBinder<T>> wBinders;
    final List<LocalSidFunctionReadBinder> rBinders;


    LocalSidFunctionBindingRegistry() {
        wBinders = new ArrayList<>();
        rBinders = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public void registerWriteFunctionType(@Nonnull final LocalSidFunctionWriteBinder binder) {
        checkNotNull(binder, "Cannot register null binder");
        if (!isFunctionRegistered(binder)) {
            wBinders.add(binder);
        } else {
            LOG.warn("Binder for class already registered. Canceling registration for {}.", binder);
        }
    }

    private boolean isFunctionRegistered(@Nonnull final LocalSidFunctionWriteBinder binder) {
        return wBinders.stream().parallel().anyMatch(locBinder -> locBinder.getClass().equals(binder.getClass()));
    }

    @SuppressWarnings("unchecked")
    public void registerReadFunctionType(@Nonnull final LocalSidFunctionReadBinder binder) {
        checkNotNull(binder, "Cannot register null binder");
        if (!isFunctionRegistered(binder)) {
            rBinders.add(binder);
        } else {
            LOG.warn("Binder for class already registered. Canceling registration for {}.", binder);
        }
    }

    private boolean isFunctionRegistered(@Nonnull final LocalSidFunctionReadBinder binder) {
        return rBinders.stream().parallel().anyMatch(locBinder -> locBinder.getClass().equals(binder.getClass()));
    }
}
