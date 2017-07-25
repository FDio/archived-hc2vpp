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

import com.google.common.base.CaseFormat;
import io.fd.hc2vpp.docs.api.VppApiMessage;


public interface VppApiUtils {

    static String generateVppApiDocLink(final String version, final String vppMessageName) {
        //https://docs.fd.io/vpp/17.07/d9/d1d/structvl__api__create__subif__t.html
        // links are using double underscore
        //final String doubleUnderscoreApiName = vppApi.replace("_", "__");
        //return format("https://docs.fd.io/vpp/%s/d9/d1d/structvl__api__%s__t.html", version, doubleUnderscoreApiName);

        // FIXME - generateLink has dynamic part that can be resolved from api name
        return "https://docs.fd.io/vpp/17.07/annotated.html";
    }

    default VppApiMessage fromJvppApi(final String version, final String jvppMethodName) {
        final String vppMessageName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, jvppMethodName);
        return new VppApiMessage(vppMessageName, generateVppApiDocLink(version, vppMessageName));
    }
}
