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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Index from module file name to git generateLink
 */
class ModelLinkIndex implements LinkGenerator {

    private final Map<String, String> modelLinkIndex;

    /**
     * @param projectRoot for ex.: /home/jsrnicek/Projects/hc2vpp
     * @param version     for ex.: 17.07 to get generateLink for correct branch
     */
    ModelLinkIndex(final String projectRoot, final String version) {
        modelLinkIndex = buildIndex(projectRoot, version);
    }

    private static String key(String raw) {
        return raw.substring(raw.lastIndexOf("/"))
                .replace("/", "")
                .replace(".yang", "");
    }

    String linkForModel(final String model, final String revision) {
        // TODO - figure out how to get revision for model in src,to use YangModelKey
        // if not local model,add generateLink to ietf datatracker
        return Optional.ofNullable(modelLinkIndex.get(model + "@" + revision))
                .orElse(Optional.ofNullable(modelLinkIndex.get(model))
                        .orElse("https://datatracker.ietf.org/"));
    }

    private Map<String, String> buildIndex(final String projectRoot, final String version) {
        try {
            return Files.walk(Paths.get(projectRoot))
                    .filter(path -> path.toString().contains("src/main/yang"))
                    .filter(path -> path.toString().endsWith(".yang"))
                    .map(Path::toString)
                    .map(s -> s.replace(projectRoot, ""))
                    .distinct()
                    .collect(Collectors.toMap(ModelLinkIndex::key, o -> generateLink(o, version)));
        } catch (IOException e) {
            throw new IllegalStateException(format("%s not found", projectRoot), e);
        }
    }
}
