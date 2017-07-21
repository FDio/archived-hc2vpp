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


import static java.lang.String.format;

import io.fd.hc2vpp.docs.api.Operation;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans provided class for reference under specified crud method
 */
public class CoverageScanner {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageScanner.class);

    private final Class<?> classToScan;
    private final Operation.CrudOperation crudOperation;
    private final Class<?> referenceClass;

    public CoverageScanner(final Class<?> classToScan,
                           final Operation.CrudOperation crudOperation,
                           final Class<?> referenceClass) {
        this.classToScan = classToScan;
        this.crudOperation = crudOperation;
        this.referenceClass = referenceClass;
    }

    static ClassReader loadClass(String className) {
        try (InputStream classStream =
                     CoverageScanner.class.getClassLoader().getResourceAsStream(className + ".class")) {
            return new ClassReader(classStream);
        } catch (IOException e) {
            throw new IllegalStateException(format("Unable to load bytecode for class %s", className), e);
        }
    }

    public Set<PluginMethodReference> scan() {
        LOG.debug("Scanning class {}", classToScan.getName());
        final ClassReader classReader = loadClass(byteCodeStyleReference(classToScan.getName()));
        final Set<PluginMethodReference> foundReferences = Collections.synchronizedSet(new HashSet<>());
        classReader.accept(new MethodDelegatingClassVisitor(byteCodeStyleReference(classToScan.getName()),
                crudOperation.getMethodReference(), byteCodeStyleReference(referenceClass.getPackage().getName()),
                foundReferences, null), ClassReader.SKIP_DEBUG);
        return foundReferences;
    }

    // converts java style reference to bytecode-style name(with slashes instead of dots)
    private static String byteCodeStyleReference(final String javaStyleName) {
        return javaStyleName.replace(".", "/");
    }
}
