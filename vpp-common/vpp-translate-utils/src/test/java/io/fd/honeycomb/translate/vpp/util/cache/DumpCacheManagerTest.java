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

package io.fd.honeycomb.translate.vpp.util.cache;

import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpPostProcessingFunction;
import io.fd.vpp.jvpp.core.dto.IpDetails;
import io.fd.vpp.jvpp.core.dto.IpDetailsReplyDump;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DumpCacheManagerTest {

    private static final String KEY = "cacheKey";

    @Mock
    private EntityDumpExecutor<IpDetailsReplyDump, Void> executor;

    @Mock
    private InstanceIdentifier identifier;

    private DumpCacheManager<IpDetailsReplyDump, Void> managerPositive;
    private DumpCacheManager<IpDetailsReplyDump, Void> managerPositiveWithPostProcessing;
    private DumpCacheManager<IpDetailsReplyDump, Void> managerNegative;
    private ModificationCache cache;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        managerPositive =
                new DumpCacheManager.DumpCacheManagerBuilder<IpDetailsReplyDump, Void>()
                        .withExecutor(executor)
                        .build();

        managerPositiveWithPostProcessing = new DumpCacheManager.DumpCacheManagerBuilder<IpDetailsReplyDump, Void>()
                .withExecutor(executor)
                .withPostProcessingFunction(createPostProcessor())
                .build();

        managerNegative =
                new DumpCacheManager.DumpCacheManagerBuilder<IpDetailsReplyDump, Void>()
                        .withExecutor(executor)
                        .build();

        cache = new ModificationCache();
    }

    /**
     * This test verify full dump-caching cycle
     */
    @Test
    public void testCaching() throws ReadFailedException {
        final IpDetailsReplyDump stage1Data = new IpDetailsReplyDump();

        // executor cant return null data
        when(executor.executeDump(identifier, NO_PARAMS)).thenReturn(new IpDetailsReplyDump());

        final Optional<IpDetailsReplyDump> stage1Optional = managerNegative.getDump(identifier, KEY, cache, NO_PARAMS);

        // this is first call so instance should be from executor
        // and it should be cached after calling executor
        assertEquals(true, stage1Optional.isPresent());
        assertEquals(stage1Data, stage1Optional.get());
        assertEquals(true, cache.containsKey(KEY));
        assertEquals(stage1Data, cache.get(KEY));

        //rebind executor with other data
        IpDetailsReplyDump stage2LoadedDump = new IpDetailsReplyDump();
        when(executor.executeDump(identifier, NO_PARAMS)).thenReturn(stage2LoadedDump);

        final Optional<IpDetailsReplyDump> stage2Optional = managerPositive.getDump(identifier, KEY, cache, NO_PARAMS);

        assertEquals(true, stage2Optional.isPresent());
        assertEquals(stage2LoadedDump, stage2Optional.get());

        //rebind executor with other data
        IpDetailsReplyDump stage3LoadedDump = new IpDetailsReplyDump();
        when(executor.executeDump(identifier, NO_PARAMS)).thenReturn(stage3LoadedDump);

        final Optional<IpDetailsReplyDump> stage3Optional = managerPositive.getDump(identifier, KEY, cache, NO_PARAMS);
        assertEquals(true, stage3Optional.isPresent());
        //check if it returns instance cached from previous stage
        assertEquals(stage2LoadedDump, stage3Optional.get());
    }

    @Test
    public void testPostprocessing() throws ReadFailedException {
        IpDetailsReplyDump dump = new IpDetailsReplyDump();
        IpDetails details = new IpDetails();
        details.swIfIndex = 2;
        dump.ipDetails.add(details);

        when(executor.executeDump(identifier, null)).thenReturn(dump);

        Optional<IpDetailsReplyDump> optionalDump =
                managerPositiveWithPostProcessing.getDump(identifier, KEY, cache, NO_PARAMS);

        assertEquals(true, optionalDump.isPresent());
        assertEquals(1, optionalDump.get().ipDetails.size());
        assertEquals(7, optionalDump.get().ipDetails.get(0).swIfIndex);
    }

    private EntityDumpPostProcessingFunction<IpDetailsReplyDump> createPostProcessor() {
        return ipDetailsReplyDump -> {
            IpDetailsReplyDump modified = new IpDetailsReplyDump();

            for (IpDetails detail : ipDetailsReplyDump.ipDetails) {
                IpDetails modifiedDetail = new IpDetails();
                modifiedDetail.swIfIndex = detail.swIfIndex + 5;

                modified.ipDetails.add(modifiedDetail);
            }

            return modified;
        };
    }
}