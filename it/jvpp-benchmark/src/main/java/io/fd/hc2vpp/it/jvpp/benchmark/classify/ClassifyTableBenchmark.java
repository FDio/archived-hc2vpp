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

package io.fd.hc2vpp.it.jvpp.benchmark.classify;

import io.fd.hc2vpp.it.jvpp.benchmark.util.JVppBenchmark;
import io.fd.jvpp.JVppRegistry;
import io.fd.jvpp.core.JVppCoreImpl;
import io.fd.jvpp.core.dto.ClassifyAddDelTableReply;
import io.fd.jvpp.core.future.FutureJVppCoreFacade;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassifyTableBenchmark extends JVppBenchmark {
    private static final Logger LOG = LoggerFactory.getLogger(ClassifyTableBenchmark.class);

    @Param( {"100"})
    private int tableSetSize;

    private FutureJVppCoreFacade jvppCore;
    private ClassifyTableProvider classifyTableProvider;

    @Benchmark
    public ClassifyAddDelTableReply testCreate() throws ExecutionException, InterruptedException {
        // Caller may want to process reply, so return it to prevent JVM from dead code elimination
        return jvppCore.classifyAddDelTable(classifyTableProvider.next()).toCompletableFuture().get();
    }

    @Override
    protected void iterationSetup() {
        classifyTableProvider = new ClassifyTableProviderImpl(tableSetSize);
    }

    // Sonar reports unclosed resources, but jvpp connection is closed in JVppBenchmark.tearDown.
    // It is only a benchmark, so if JMH would crash and not call tearDown, then we don't really care.
    @SuppressWarnings("squid:S2095")
    @Override
    protected void connect(final JVppRegistry registry) throws IOException {
        jvppCore = new FutureJVppCoreFacade(registry, new JVppCoreImpl());
    }

    @Override
    protected void disconnect() throws Exception {
        jvppCore.close();
    }
}
