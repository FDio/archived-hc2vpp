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

public interface LinkGenerator {

    static String resolveBranch(final String version) {
        if (version.contains("SNAPSHOT")) {
            return "master";
        } else {
            return "stable%2F" + version.replace(".", "");
        }
    }

    default String generateLink(final String raw, final String version) {
        //https://git.fd.io/hc2vpp/tree/interface-role/api/src/main/yang/interface-role@2017-06-15.yang?h=stable%2F1707
        return "https://git.fd.io/hc2vpp/tree" + raw + "?h=" + resolveBranch(version);
    }
}
