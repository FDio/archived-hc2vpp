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

import static io.fd.hc2vpp.it.jvpp.benchmark.AclUpdateBenchmark.InterfaceMode.L2;
import static io.fd.hc2vpp.it.jvpp.benchmark.AclUpdateBenchmark.InterfaceMode.L3;

import com.google.common.io.CharStreams;
import io.fd.vpp.jvpp.JVppRegistryImpl;
import io.fd.vpp.jvpp.acl.JVppAclImpl;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceSetAclList;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import io.fd.vpp.jvpp.core.JVppCoreImpl;
import io.fd.vpp.jvpp.core.dto.BridgeDomainAddDel;
import io.fd.vpp.jvpp.core.dto.CreateLoopback;
import io.fd.vpp.jvpp.core.dto.CreateLoopbackReply;
import io.fd.vpp.jvpp.core.dto.SwInterfaceAddDelAddress;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetFlags;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetL2Bridge;
import io.fd.vpp.jvpp.core.future.FutureJVppCoreFacade;
import io.fd.vpp.jvpp.dto.JVppReply;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
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

    @Param( {"L3"})
    private InterfaceMode mode;

    private AclProvider aclProvider;
    private JVppRegistryImpl registry;
    private FutureJVppAclFacade jvppAcl;
    private FutureJVppCoreFacade jvppCore;

    @Benchmark
    public void testMethod() throws Exception {
        jvppAcl.aclAddReplace(aclProvider.next()).toCompletableFuture().get();
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
        jvppCore = new FutureJVppCoreFacade(registry, new JVppCoreImpl());
        jvppAcl = new FutureJVppAclFacade(registry, new JVppAclImpl());
        LOG.info("Successfully connected to JVPP");
    }

    private void disconnect() throws Exception {
        LOG.info("Disconnecting ...");
        jvppAcl.close();
        jvppCore.close();
        registry.close();
        LOG.info("Successfully disconnected ...");
    }

    /**
     * Initializes loopback interface, creates ACL and assigns it to loop0.
     */
    private void initAcl()
        throws ExecutionException, InterruptedException {
        // Create loopback interface
        final CreateLoopbackReply loop0 = invoke(jvppCore.createLoopback(new CreateLoopback()));

        // Enable loop0
        final SwInterfaceSetFlags flags = new SwInterfaceSetFlags();
        flags.adminUpDown = 1;
        flags.swIfIndex = loop0.swIfIndex;
        invoke(jvppCore.swInterfaceSetFlags(flags));

        if (L3.equals(mode)) {
            // Assign IP to loop0
            final SwInterfaceAddDelAddress address = new SwInterfaceAddDelAddress();
            address.address = new byte[]{1,0,0,0};
            address.addressLength = 8;
            address.isAdd = 1;
            address.swIfIndex = loop0.swIfIndex;
            invoke(jvppCore.swInterfaceAddDelAddress(address));
        } else if (L2.equals(mode)) {
            // Create bridge domain 1
            final BridgeDomainAddDel bd = new BridgeDomainAddDel();
            bd.bdId = 1;
            bd.isAdd = 1;
            invoke(jvppCore.bridgeDomainAddDel(bd));

            // Assign loop0 to BD1:
            final SwInterfaceSetL2Bridge loop0Bridge = new SwInterfaceSetL2Bridge();
            loop0Bridge.bdId = bd.bdId;
            loop0Bridge.rxSwIfIndex = loop0.swIfIndex;
            loop0Bridge.enable = 1; // set L2 mode
            invoke(jvppCore.swInterfaceSetL2Bridge(loop0Bridge));
        }

        // Create ACL
        final int aclId = invoke(jvppAcl.aclAddReplace(aclProvider.next())).aclIndex;

        // Assign the ACL to loop0 interface
        final AclInterfaceSetAclList aclList = new AclInterfaceSetAclList();
        aclList.swIfIndex = loop0.swIfIndex;
        aclList.count = 1;
        aclList.nInput = 1;
        aclList.acls = new int[] {aclId};
        invoke(jvppAcl.aclInterfaceSetAclList(aclList));

        // Use ACL index in subsequent executions of aclProvider.next() method
        aclProvider.setAclIndex(aclId);
    }

    public enum InterfaceMode {
        L2, L3
    }

    private static <R extends JVppReply<?>> R invoke(final CompletionStage<R> completionStage)
        throws ExecutionException, InterruptedException {
        return completionStage.toCompletableFuture().get();
    }
}
