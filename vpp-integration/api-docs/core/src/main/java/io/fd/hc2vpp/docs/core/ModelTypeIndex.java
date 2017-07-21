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

package io.fd.hc2vpp.docs.core;

import static java.util.stream.Collectors.toMap;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

/**
 * Maps namespaces to models
 */
class ModelTypeIndex {

    private final Map<YangModelKey, String> namespaceToModuleIndex;

    ModelTypeIndex() throws IOException {
        namespaceToModuleIndex = collectAllModules(this.getClass().getClassLoader())
                .stream()
                .collect(toMap(YangModelKey::new, YangModuleInfo::getName));
    }

    private static YangModelBindingProvider getModuleBindingProviderInstance(final Class<?> aClass) {
        try {
            return (YangModelBindingProvider) aClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Class<?> loadClass(final ClassLoader classLoader, final String name) {
        try {
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);

        }
    }

    private static List<String> loadResource(final URL url) {
        try {
            return Resources.readLines(url, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    String namespaceToModule(final String namespace,
                             final String revision) {
        return namespaceToModuleIndex.get(new YangModelKey(namespace, revision));
    }

    private Set<YangModuleInfo> collectAllModules(final ClassLoader classLoader) throws IOException {
        return Collections.list(classLoader.getResources("META-INF/services/" +
                YangModelBindingProvider.class.getName()))
                .stream()
                .map(ModelTypeIndex::loadResource)
                .flatMap(Collection::stream)
                .map(name -> loadClass(classLoader, name))
                .map(ModelTypeIndex::getModuleBindingProviderInstance)
                .map(YangModelBindingProvider::getModuleInfo)
                .collect(Collectors.toSet());
    }
}
