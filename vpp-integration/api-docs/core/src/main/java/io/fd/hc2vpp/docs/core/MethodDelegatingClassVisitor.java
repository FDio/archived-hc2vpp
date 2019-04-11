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


import java.util.Set;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodDelegatingClassVisitor extends ClassVisitor {

    private final String currentClass;
    private final String methodName;
    private final String reference;
    private final Set<PluginMethodReference> foundReferences;
    private final Set<String> allreadyProcessedLocalMethods;

    public MethodDelegatingClassVisitor(String currentClass,
                                        String methodName,
                                        String reference,
                                        Set<PluginMethodReference> foundReferences,
                                        Set<String> allreadyProcessedLocalMethods) {
        super(Opcodes.ASM7);
        this.currentClass = currentClass;
        this.methodName = methodName;
        this.reference = reference;
        this.foundReferences = foundReferences;
        this.allreadyProcessedLocalMethods = allreadyProcessedLocalMethods;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals(methodName)) {
            return new MethodPluginCoverageVisitor(currentClass, foundReferences, reference,
                    allreadyProcessedLocalMethods);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }
}
