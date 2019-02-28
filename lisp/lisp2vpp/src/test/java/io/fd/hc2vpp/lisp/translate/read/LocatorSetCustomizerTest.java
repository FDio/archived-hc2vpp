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

package io.fd.hc2vpp.lisp.translate.read;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.jvpp.core.dto.OneLocatorSetDetails;
import io.fd.jvpp.core.dto.OneLocatorSetDetailsReplyDump;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.Lisp;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.LispState;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.locator.sets.grouping.LocatorSetsBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.locator.sets.grouping.locator.sets.LocatorSetBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class LocatorSetCustomizerTest
        extends LispInitializingListReaderCustomizerTest<LocatorSet, LocatorSetKey, LocatorSetBuilder>
        implements LispInitTest {

    private static final String LOC_1_PATH = "/lisp:lisp-state" +
            "/lisp:lisp-feature-data" +
            "/lisp:locator-sets" +
            "/lisp:locator-set[lisp:name='loc1']";
    private InstanceIdentifier<LocatorSet> emptyId;
    private InstanceIdentifier<LocatorSet> validId;

    public LocatorSetCustomizerTest() {
        super(LocatorSet.class, LocatorSetsBuilder.class);
    }

    @Before
    public void init() {
        emptyId = InstanceIdentifier.create(LocatorSet.class);
        validId = InstanceIdentifier.create(LocatorSets.class).child(LocatorSet.class, new LocatorSetKey("loc-set"));

        defineDumpData();
        defineMapping(mappingContext, "loc-set", 1, "locator-set-context");
        mockLispEnabled();
    }

    private void defineDumpData() {
        OneLocatorSetDetailsReplyDump dump = new OneLocatorSetDetailsReplyDump();
        OneLocatorSetDetails detail = new OneLocatorSetDetails();
        detail.context = 4;
        detail.lsName = "loc-set".getBytes(StandardCharsets.UTF_8);
        detail.lsIndex = 1;

        dump.oneLocatorSetDetails = ImmutableList.of(detail);

        when(api.oneLocatorSetDump(any())).thenReturn(future(dump));
    }


    @Test
    public void readCurrentAttributes() throws Exception {
        LocatorSetBuilder builder = new LocatorSetBuilder();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);

        assertNotNull(builder);
        assertEquals("loc-set", builder.getName());
        assertEquals("loc-set", builder.key().getName());
    }

    @Test
    public void getAllIds() throws Exception {
        final List<LocatorSetKey> keys = getCustomizer().getAllIds(emptyId, ctx);

        assertEquals(1, keys.size());
        assertEquals("loc-set", keys.get(0).getName());
    }

    @Test
    public void testInit(@InjectTestData(resourcePath = "/locator-set.json", id = LOC_1_PATH) LocatorSet locatorSet) {
        final LocatorSetKey loc1Key = new LocatorSetKey("loc1");
        final KeyedInstanceIdentifier<LocatorSet, LocatorSetKey> operationalPath = InstanceIdentifier.create(LispState.class)
                .child(LispFeatureData.class)
                .child(LocatorSets.class)
                .child(LocatorSet.class, loc1Key);

        final KeyedInstanceIdentifier<LocatorSet, LocatorSetKey> configPath = InstanceIdentifier.create(Lisp.class)
                .child(LispFeatureData.class)
                .child(LocatorSets.class)
                .child(LocatorSet.class, loc1Key);

        invokeInitTest(operationalPath, locatorSet, configPath, locatorSet);
    }

    @Override
    protected ReaderCustomizer<LocatorSet, LocatorSetBuilder> initCustomizer() {
        return new LocatorSetCustomizer(api, lispStateCheckService);
    }
}
