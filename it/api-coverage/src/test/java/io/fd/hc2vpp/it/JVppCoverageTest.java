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

package io.fd.hc2vpp.it;

import io.fd.vpp.jvpp.acl.JVppAcl;
import io.fd.vpp.jvpp.core.JVppCore;
import io.fd.vpp.jvpp.gtpu.JVppGtpu;
import io.fd.vpp.jvpp.ioamexport.JVppIoamexport;
import io.fd.vpp.jvpp.ioampot.JVppIoampot;
import io.fd.vpp.jvpp.ioamtrace.JVppIoamtrace;
import io.fd.vpp.jvpp.nat.JVppNat;
import io.fd.vpp.jvpp.pppoe.JVppPppoe;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Scanner;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JVppCoverageTest {

    private static final Logger LOG = LoggerFactory.getLogger(JVppCoverageTest.class);

    @Test
    public void coverageTest() throws IOException {
        final Class[] apis = new Class[] {
            JVppAcl.class,
            JVppCore.class,
            JVppGtpu.class,
            JVppIoamexport.class,
            JVppIoampot.class,
            JVppIoamtrace.class,
            JVppNat.class,
            JVppPppoe.class
        };
        int covered = 0;
        int methods = 0;
        for (Class api : apis) {
            final Method[] declaredMethods = api.getDeclaredMethods();
            methods += declaredMethods.length - 1; // excluding send method
            covered += coverage(api, declaredMethods);
        }
        LOG.info("#methods={} covered={} ({}%)", methods, covered, (100.0 * covered) / methods);
    }

    private static int coverage(final Class api, final Method[] methods) throws IOException {
        int covered = 0;
        int nMethods = methods.length - 1; // excluding send method
        for (Method method : methods) {
            LOG.info(method.getName());
            if (isMethodCovered(method.getName())) {
                covered++;
            }
        }
        covered--; // excluding send
        LOG.info("{}: #methods={} covered={} ({}%)", api, nMethods, covered, (100.0 * covered) / nMethods);
        return covered;
    }

    private static boolean isMethodCovered(final String name) throws IOException {
        final Runtime rt = Runtime.getRuntime();
        // TODO (grep per api name is slow):
        // scan all java files for jvpp invocations, make a set/map and use it for usage lookup
        final String[] cmd =
            {"/bin/sh", "-c", "grep -rn /home/m/hc2vpp --include *.java -rn . -e \"" + name + "\" | wc -l"};
        final Process proc = rt.exec(cmd);
        final Scanner sc = new Scanner(proc.getInputStream());
        final int nRefs = sc.nextInt();
        LOG.info("Method {} was referenced {}", name, nRefs);
        return nRefs > 0;
    }
}
