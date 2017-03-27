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

package io.fd.hc2vpp.model.test;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertNotNull;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.data.impl.leafref.LeafRefContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Tests whether LeafRefContext in is constructable from standard set of models
 */
public class LeafRefContextTest {

    private static final String YANG_BA_PROVIDER_PATH = "META-INF/services/" + YangModelBindingProvider.class.getName();

    @Test
    public void testLeafRefContextCreation() {
        assertNotNull(LeafRefContext.create(context()));
    }

    private SchemaContext context() {

        ModuleInfoBackedContext ctx = ModuleInfoBackedContext.create();
        final Set<YangModuleInfo> modules = modules();
        checkState(!modules.isEmpty(), "No modules found");
        ctx.addModuleInfos(modules);

        return ctx.getSchemaContext();
    }

    private Set<YangModuleInfo> modules() {
        try {
            return Collections.list(getClass().getClassLoader().getResources(YANG_BA_PROVIDER_PATH))
                    .stream()
                    .map(LeafRefContextTest::urlToString)
                    .flatMap(content -> Arrays.stream(content.split("\n")))
                    .filter(line -> !Strings.isNullOrEmpty(line.trim()))
                    .map(LeafRefContextTest::loadClass)
                    .map(LeafRefContextTest::getInstance)
                    .map(YangModelBindingProvider.class::cast)
                    .map(YangModelBindingProvider::getModuleInfo)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load binding providers from path: " + YANG_BA_PROVIDER_PATH, e);
        }
    }

    private static Object getInstance(@Nonnull final Class<?> aClass) {
        try {
            return aClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to create instance of " + aClass);
        }
    }

    private static Class<?> loadClass(@Nonnull final String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to load class: " + className, e);
        }
    }

    private static String urlToString(@Nonnull final URL url) {
        try {
            return Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read resource from: " + url, e);
        }
    }
}
