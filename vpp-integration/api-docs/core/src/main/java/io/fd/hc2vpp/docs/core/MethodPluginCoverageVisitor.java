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

import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MethodPluginCoverageVisitor extends MethodVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(MethodPluginCoverageVisitor.class);

    private final String currentClass;
    private final Set<PluginMethodReference> foundReferences;
    private final String reference;
    private final Set<String> allreadyProcessedLocal;

    public MethodPluginCoverageVisitor(String currentClass, Set<PluginMethodReference> foundReferences,
                                       String reference,
                                       Set<String> allreadyProcessedLocal) {
        super(Opcodes.ASM7);
        this.currentClass = currentClass;
        this.foundReferences = foundReferences;
        this.reference = reference;
        // if nonnull then reuse
        this.allreadyProcessedLocal = allreadyProcessedLocal == null
                ? new HashSet<>()
                : allreadyProcessedLocal;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
        final String normalizedOwner = owner.replace(";", "").replace("[L", "");
        if (normalizedOwner.contains(reference)) {
            // reference was found directly in method code
            foundReferences.add(new PluginMethodReference(currentClass, owner, name));
        } else {
            if (normalizedOwner.contains("io/fd")) {
                // filter just our method to reduce scope
                if (!normalizedOwner.equals(currentClass)) {
                    LOG.debug("Processing non-current {}.{}()", normalizedOwner, name);
                    // method call is from different class than currently processed one
                    ClassReader classReader = CoverageScanner.loadClass(normalizedOwner);
                    classReader.accept(new MethodDelegatingClassVisitor(normalizedOwner, name, reference,
                            foundReferences,
                            allreadyProcessedLocal), ClassReader.SKIP_DEBUG);
                } else {
                    LOG.debug("Processing current {}.{}()", normalizedOwner, name);
                    // other methods in same class that are used in visited method
                    String fullyQualifiedMethodName = fullyQualifiedMethodName(normalizedOwner, name, desc);
                    if (allreadyProcessedLocal.contains(fullyQualifiedMethodName)) {
                        //skip already processed local methods to prevent stack overflow
                        return;
                    }
                    allreadyProcessedLocal.add(fullyQualifiedMethodName);

                    ClassReader classReader = CoverageScanner.loadClass(normalizedOwner);
                    classReader.accept(new MethodDelegatingClassVisitor(normalizedOwner, name, reference,
                            foundReferences,
                            allreadyProcessedLocal), ClassReader.SKIP_DEBUG);
                }
            }
        }
        super.visitMethodInsn(opcode, owner, name, desc, isInterface);
    }

    private String fullyQualifiedMethodName(String owner, String name, String desc) {
        return format("%s_%s_%s", owner, name, desc);
    }
}
