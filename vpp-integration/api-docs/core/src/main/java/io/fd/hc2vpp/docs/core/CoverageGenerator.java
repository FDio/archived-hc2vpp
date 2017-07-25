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


import static io.fd.hc2vpp.docs.api.Operation.CrudOperation.DELETE;
import static io.fd.hc2vpp.docs.api.Operation.CrudOperation.UPDATE;
import static io.fd.hc2vpp.docs.api.Operation.CrudOperation.WRITE;
import static java.lang.String.format;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import io.fd.hc2vpp.docs.api.CoverageUnit;
import io.fd.hc2vpp.docs.api.JavaApiMessage;
import io.fd.hc2vpp.docs.api.Operation;
import io.fd.hc2vpp.docs.api.PluginCoverage;
import io.fd.hc2vpp.docs.api.YangType;
import io.fd.honeycomb.translate.write.WriterFactory;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoverageGenerator implements VppApiUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageGenerator.class);

    private final CollectingWriterBuilder writerBuilder;

    public CoverageGenerator() {
        writerBuilder = new CollectingWriterBuilder();
    }

    public PluginCoverage generateConfigCoverage(final Class<?> pluginClass,
                                                 final String version,
                                                 final List<Module> scannedModules,
                                                 final YangTypeLinkIndex yangTypeIndex,
                                                 final ClassPathTypeIndex classPathIndex) {
        LOG.info("Generating config VPP API to Yang mapping for plugin {}", pluginClass);
        getInjectedWriterFactories(scannedModules).forEach(writerFactory -> writerFactory.init(writerBuilder));

        final Set<CoverageUnit> coverageUnits = writerBuilder.getWriteHandlers().stream()
                .flatMap(handler -> {
                    // extracts customizer class from handler
                    final Class<?> customizerClass = getCustomizerClass(handler.getWriter());

                    // scans within write method
                    final Set<PluginMethodReference> writeReferences =
                            new CoverageScanner(customizerClass, WRITE, pluginClass).scan();

                    // scans within update method
                    final Set<PluginMethodReference> updateReferences =
                            new CoverageScanner(customizerClass, UPDATE, pluginClass).scan();

                    // scans within delete method
                    final Set<PluginMethodReference> deleteReferences =
                            new CoverageScanner(customizerClass, DELETE, pluginClass).scan();

                    return Stream.of(writeReferences.stream(), updateReferences.stream(), deleteReferences.stream())
                            .flatMap(pluginMethodReferenceStream -> pluginMethodReferenceStream)
                            .map(reference -> {
                                final CoverageUnit.CoverageUnitBuilder builder = new CoverageUnit.CoverageUnitBuilder();

                                // binds vpp api name and generateLink bind with version
                                builder.setVppApi(fromJvppApi(version, reference.getName()));

                                //binds java api reference
                                builder.setJavaApi(new JavaApiMessage(reference.getName()));

                                // binds Yang types with links from pre-build index
                                // TODO - use deserialized yii e.g. /module:parent-node/child-node
                                builder.setYangTypes(handler.getHandledNodes().stream()
                                        .map(type -> new YangType(type, yangTypeIndex.getLinkForType(type)))
                                        .collect(Collectors.toList()));

                                final String callerClassLink = classPathIndex.linkForClass(reference.getCaller());
                                final List<Operation> supportedOperations = new LinkedList<>();
                                if (writeReferences.contains(reference)) {
                                    supportedOperations.add(new Operation(callerClassLink, WRITE));
                                }

                                if (updateReferences.contains(reference)) {
                                    supportedOperations.add(new Operation(callerClassLink, UPDATE));
                                }

                                if (deleteReferences.contains(reference)) {
                                    supportedOperations.add(new Operation(callerClassLink, DELETE));
                                }
                                return builder.setSupportedOperations(supportedOperations).build();
                            });
                }).collect(Collectors.toSet());

        return new PluginCoverage(pluginClass.getSimpleName(), coverageUnits, true);
    }

    private static Class<?> getCustomizerClass(final Object handler) {
        try {
            final Set<Field> customizerFields =
                    ReflectionUtils.getAllFields(handler.getClass(), field -> "customizer".equals(field.getName()));
            final Field customizerField = customizerFields.iterator().next();
            customizerField.setAccessible(true);
            return customizerField.get(handler).getClass();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(format("Unable to get customizer from %s ", handler), e);
        }
    }

    private static Set<WriterFactory> getInjectedWriterFactories(final List<Module> scannedModules) {
        Injector injector = Guice.createInjector(scannedModules);
        TypeLiteral<Set<WriterFactory>> writerFactoryType = new TypeLiteral<Set<WriterFactory>>() {
        };
        return injector.getInstance(Key.get(writerFactoryType));
    }
}
