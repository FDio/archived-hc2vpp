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
import java.util.stream.Collectors;


/**
 * Index of java classes to relative absolute paths within repository. Used to generate Git links for binding classes of
 * VPP apis
 */
public class ClassPathTypeIndex implements LinkGenerator {

    private static final String JAVA_SOURCE_FOLDER = "src/main/java";
    private static final int JAVA_SOURCE_FOLDER_NAME_LENGTH = JAVA_SOURCE_FOLDER.length() + 1;

    /**
     * <li>key - fully qualified class name value</li><li>value - path within codebase/repository</li>
     */
    private final Map<String, String> index;

    public ClassPathTypeIndex(final String projectRoot) {
        index = buildIndex(projectRoot);
    }

    /**
     * <li>input format - LOCAL_ROOT/hc2vpp/module/src/main/java/fully/qualified/class/name/Class.java</li><li>output
     * format - fully.qualified.class.name.Class</li>
     */
    private static String key(String raw) {
        return raw.substring(raw.indexOf(JAVA_SOURCE_FOLDER) + JAVA_SOURCE_FOLDER_NAME_LENGTH)
                .replace("/", ".")
                .replace(".java", "");
    }

    public String linkForClass(final String clazz) {
        return index.get(clazz.replace("/", "."));
    }

    private Map<String, String> buildIndex(final String projectRoot) {
        try {
            return Files.walk(Paths.get(projectRoot))
                    .filter(path -> path.toString().contains("src/main/java"))
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(Path::toString)
                    .map(s -> s.replace(projectRoot, ""))
                    .distinct()
                    .collect(Collectors.toMap(ClassPathTypeIndex::key, o -> generateLink(o)));
        } catch (IOException e) {
            throw new IllegalStateException(format("%s not found", projectRoot), e);
        }
    }
}
