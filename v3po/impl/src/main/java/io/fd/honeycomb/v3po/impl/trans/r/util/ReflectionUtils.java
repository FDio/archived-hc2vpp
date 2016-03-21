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

package io.fd.honeycomb.v3po.impl.trans.r.util;

import com.google.common.base.Optional;
import java.lang.reflect.Method;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Reflection based utilities
 */
public final class ReflectionUtils {

    private ReflectionUtils() {}

    /**
     * Find a specific method using reflection
     *
     * @param managedType Class object to find method in
     * @param prefix Method name prefix used when finding the method. Case does not matter.
     * @param paramTypes List of input argument types
     * @param retType Return type
     *
     * @return Found method or Optional.absent() if there's no such method
     */
    @Nonnull
    public static Optional<Method> findMethodReflex(@Nonnull final Class<?> managedType,
                                                    @Nonnull final String prefix,
                                                    @Nonnull final List<Class<?>> paramTypes,
                                                    @Nonnull final Class<?> retType) {
        for (Method method : managedType.getMethods()) {
            if(isMethodMatch(prefix, paramTypes, retType, method)) {
                return Optional.of(method);
            }
        }

        return Optional.absent();
    }

    private static boolean isMethodMatch(final @Nonnull String prefix,
                                         final @Nonnull List<Class<?>> paramTypes,
                                         final @Nonnull Class<?> retType, final Method method) {
        if (!method.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
            return false;
        }

        final Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != paramTypes.size()) {
            return false;
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            if (!parameterTypes[i].isAssignableFrom(paramTypes.get(i))) {
                return false;
            }
        }

        if (!method.getReturnType().equals(retType)) {
            return false;
        }

        return true;
    }
}
