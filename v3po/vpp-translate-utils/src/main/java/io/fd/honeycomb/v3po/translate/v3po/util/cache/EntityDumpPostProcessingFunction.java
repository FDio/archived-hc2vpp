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

package io.fd.honeycomb.v3po.translate.v3po.util.cache;

import java.util.function.Function;
import org.openvpp.jvpp.dto.JVppReplyDump;

/**
 * Generic interface for class that are post-processing data dumped from vpp
 */
@FunctionalInterface
public interface EntityDumpPostProcessingFunction<T extends JVppReplyDump> extends Function<T, T> {


    /**
     * Performs postprocessing on dumped data
     *
     * @return Post-processed data
     */
    @Override
    T apply(T t);
}