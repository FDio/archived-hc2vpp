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

package io.fd.hc2vpp.docs.scripts

import groovy.text.SimpleTemplateEngine
import io.fd.hc2vpp.docs.api.*
import io.fd.hc2vpp.docs.core.ClassPathTypeIndex
import io.fd.hc2vpp.docs.core.CoverageGenerator
import io.fd.hc2vpp.docs.core.YangTypeLinkIndex
import io.fd.vpp.jvpp.acl.future.FutureJVppAcl
import io.fd.vpp.jvpp.core.future.FutureJVppCore
import io.fd.vpp.jvpp.ioamexport.future.FutureJVppIoamexport
import io.fd.vpp.jvpp.ioampot.future.FutureJVppIoampot
import io.fd.vpp.jvpp.ioamtrace.future.FutureJVppIoamtrace
import io.fd.vpp.jvpp.nsh.future.FutureJVppNsh
import io.fd.vpp.jvpp.snat.future.FutureJVppSnat

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.stream.Collectors

import static java.util.stream.Collectors.toList

/**
 * Generates VPP api to Yang node index for hc2vpp guice modules listed in api.docs.modules maven property.
 */
class ApiDocsIndexGenerator {

    private static def NL = System.lineSeparator()
    // TODO - check if list of plugin classes can be generated based on list of modules enabled for doc generation
    private static
    def PLUGIN_CLASSES = [FutureJVppCore.class, FutureJVppAcl.class, FutureJVppSnat.class, FutureJVppNsh.class,
                          FutureJVppIoamexport.class, FutureJVppIoampot.class, FutureJVppIoamtrace.class]
    private static def TABLE_PART_MARK = "|"

    /**
     * Generate coverage data for all configured coverage.modules and JVpp plugins
     * */
    public static void generate(final project, final log) {
        def loader = this.getClassLoader()

        String moduleNames = project.properties.get("api.docs.modules")
        String projectRoot = project.properties.get("project.root.folder")

        if (moduleNames.trim().isEmpty()) {
            log.info "No modules defined for ${project.name}. Skipping api-docs generation."
            return
        }

        final List<String> moduleNamesList = moduleNames.split(",")

        log.info "Reading module list for ${project.name}"
        def modules = moduleNamesList.stream()
                .map { moduleName -> moduleName.trim() }
                .map { moduleName ->
            log.info "Loading class $moduleName"
            loader.loadClass(moduleName).newInstance()
        }
        .collect(toList())

        String outPath = project.build.outputDirectory

        log.info "Generating yang type generateLink index"
        YangTypeLinkIndex yangTypeIndex = new YangTypeLinkIndex(projectRoot, project.version)
        log.info "Classpath type generateLink index"
        ClassPathTypeIndex classPathIndex = new ClassPathTypeIndex(projectRoot, project.version)

        log.info "Generating VPP API to YANG mapping"
        PLUGIN_CLASSES.stream()
                .forEach { pluginClass ->
            log.info "Generating mapping for ${pluginClass}"
            final PluginCoverage configCoverage = new CoverageGenerator()
                    .generateConfigCoverage(pluginClass, project.version, modules, yangTypeIndex, classPathIndex)
            generateJvppCoverageDoc(configCoverage, outPath, log)

            //TODO operational coverage
        }
    }

    static void generateJvppCoverageDoc(
            final PluginCoverage pluginCoverage, final String outPath, final log) {
        if (!pluginCoverage.hasCoverage()) {
            log.info "Plugin ${pluginCoverage.getPluginName()} does not have coverage data, skipping config docs generation"
            return
        }
        log.info "Generating config api docs for plugin ${pluginCoverage.getPluginName()}"
        def template = this.getClassLoader().getResource("yang_to_jvpp_template")
        def result = new SimpleTemplateEngine()
                .createTemplate(template)
                .make(["pluginName": pluginCoverage.getPluginName(), "tableContent": generateConfigTableContent(pluginCoverage.getCoverage())]).toString()

        log.debug "Docs result for ${pluginCoverage.getPluginName()}$NL$result"

        Paths.get(outPath).toFile().mkdirs()

        def outFileName
        if (pluginCoverage.isConfig()) {
            outFileName = "${normalizePluginName(pluginCoverage.getPluginName())}-yang-config-index.adoc"
        } else {
            outFileName = "${normalizePluginName(pluginCoverage.getPluginName())}-yang-operational-index.adoc"
        }

        def outFilePath = Paths.get(outPath, outFileName)

        Files.write(outFilePath, result.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE)
        log.info "Plugin ${pluginCoverage.getPluginName()} config api docs sucessfully writen to ${outFilePath.toString()}"
    }

    private static String generateConfigTableContent(final Set<CoverageUnit> coverage) {
        coverage.stream()
                .map { unit ->
            "$NL" +
                    "${vppApiWithLink(unit.vppApi)}" +
                    "${javaApi(unit.javaApi)}" +
                    "${yangTypes(unit.yangTypes)}" +
                    "${supportedOperations(unit.supportedOperations)}"
        }
        .collect(Collectors.joining(NL))
    }

    private static String vppApiWithLink(final VppApiMessage vppApi) {
        "$TABLE_PART_MARK${vppApi.link}[${vppApi.name}]$NL"
    }

    private static String javaApi(final JavaApiMessage javaApi) {
        "$TABLE_PART_MARK${javaApi.name}$NL"
    }

    private static String yangTypes(final List<YangType> yangTypes) {
        "$NL$TABLE_PART_MARK$NL ${yangTypes.stream().map { yangType -> " ${yangType.link}[${yangType.type}]" }.collect(Collectors.joining(NL))}"
    }

    private static String supportedOperations(final Collection<Operation> operations) {
        "$NL$TABLE_PART_MARK${operations.stream().map { reference -> " ${reference.link}[${reference.operation}]" }.collect(Collectors.joining(NL))}"
    }

    private static String normalizePluginName(final String name) {
        name.toLowerCase().replace(" ", "-")
    }
}
