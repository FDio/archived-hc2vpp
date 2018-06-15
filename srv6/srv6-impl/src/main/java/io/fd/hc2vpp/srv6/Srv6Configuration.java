/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.srv6;

public class Srv6Configuration {

    /**
     * Provides default locator length value for locator context. Since local SID is represented as IPv6 address, which
     * represents LOCATOR+FUNCTION. This address with total length of 128 bits is divided into these parts by defining
     * locator length. Because ietf-srv6-types model defines local SIds OpCode (function) as uint32, it leaves 96 bits
     * for the locator part. This way we can use full range of the IPv6 address without loosing any bits.
     */
    public static final Integer DEFAULT_LOCATOR_LENGTH = 96;
}
