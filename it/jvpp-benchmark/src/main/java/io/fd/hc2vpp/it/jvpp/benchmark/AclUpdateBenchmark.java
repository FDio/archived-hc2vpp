/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.it.jvpp.benchmark;

import com.google.common.io.CharStreams;
import io.fd.vpp.jvpp.JVppRegistryImpl;
import io.fd.vpp.jvpp.acl.JVppAclImpl;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceSetAclList;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@Fork(1)
@Threads(1)
@Timeout(time = 5)
@Warmup(iterations = 20, time = 2)
@Measurement(iterations = 100, time = 2)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class AclUpdateBenchmark {
    private static final Logger LOG = LoggerFactory.getLogger(AclUpdateBenchmark.class);

    @Param( {"100"})
    private int aclSize;

    @Param( {"100"})
    private int aclSetSize;

    private AclProvider aclProvider;
    private JVppRegistryImpl registry;
    private FutureJVppAclFacade jvpp;

    @Benchmark
    public void testMethod() throws Exception {
        jvpp.aclAddReplace(aclProvider.next()).toCompletableFuture().get();
    }

    @Setup(Level.Iteration)
    public void setup() throws Exception {
        initAclProvider();
        startVpp();
        connect();
        initAcl();
    }

    @TearDown(Level.Iteration)
    public void tearDown() throws Exception {
        disconnect();
        stopVpp();
    }

    private void initAclProvider() {
        aclProvider = new AclProviderImpl(aclSetSize, aclSize);
    }

    private void startVpp() throws Exception {
        LOG.info("Starting VPP ...");
        final String[] cmd = {"/bin/sh", "-c", "sudo service vpp start"};
        exec(cmd);
        LOG.info("VPP started successfully");
    }

    private void stopVpp() throws Exception {
        LOG.info("Stopping VPP ...");
        final String[] cmd = {"/bin/sh", "-c", "sudo service vpp stop"};
        exec(cmd);

        // Wait to be sure VPP was stopped.
        // Prevents VPP start failure: "vpp.service: Start request repeated too quickly".
        Thread.sleep(1500);
        LOG.info("VPP stopped successfully");

    }

    private static void exec(String[] command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
        if (process.exitValue() != 0) {
            String error_msg = "Failed to execute " + Arrays.toString(command) + ": " +
                CharStreams.toString(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
            throw new IllegalStateException(error_msg);
        }
    }

    private void connect() throws IOException {
        LOG.info("Connecting to JVPP ...");
        registry = new JVppRegistryImpl("ACLUpdateBenchmark");
        jvpp = new FutureJVppAclFacade(registry, new JVppAclImpl());
        LOG.info("Successfully connected to JVPP");
    }

    private void disconnect() throws Exception {
        LOG.info("Disconnecting ...");
        jvpp.close();
        registry.close();
        LOG.info("Successfully disconnected ...");
    }

    /**
     * Creates ACL and assigns it to local0 interface.
     */
    private void initAcl()
        throws ExecutionException, InterruptedException {
        // Create ACL
        final int aclId = jvpp.aclAddReplace(aclProvider.next()).toCompletableFuture().get().aclIndex;

        // Assign the ACL to local0 interface
        final AclInterfaceSetAclList aclList = new AclInterfaceSetAclList();
        aclList.swIfIndex = 0;
        aclList.count = 1;
        aclList.nInput = 1;
        aclList.acls = new int[] {aclId};
        jvpp.aclInterfaceSetAclList(aclList).toCompletableFuture().get();

        // Use ACL index in subsequent executions of aclProvider.next() method
        aclProvider.setAclIndex(aclId);
    }
}
