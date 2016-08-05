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

package io.fd.honeycomb.translate.v3po.vppclassifier;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.test.ContextTestUtils;
import io.fd.honeycomb.translate.v3po.test.ListReaderCustomizerTest;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.HexString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifierState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.base.attributes.ClassifySession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.base.attributes.ClassifySessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.base.attributes.ClassifySessionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.state.ClassifyTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.state.ClassifyTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.state.ClassifyTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.ClassifySessionDetails;
import org.openvpp.jvpp.core.dto.ClassifySessionDetailsReplyDump;
import org.openvpp.jvpp.core.dto.ClassifySessionDump;

public class ClassifySessionReaderTest extends
        ListReaderCustomizerTest<ClassifySession, ClassifySessionKey, ClassifySessionBuilder> {

    private static final String MATCH_1 = "00:00:00:00:00:00:01:02:03:04:05:06:00:00:00:00";
    private static final String MATCH_2 = "00:00:00:00:00:00:01:02:03:04:05:07:00:00:00:00";

    private static final int TABLE_INDEX = 1;
    private static final String TABLE_NAME = "table1";

    private NamingContext classifyTableContext;

    public ClassifySessionReaderTest() {
        super(ClassifySession.class);
    }

    @Override
    public void setUpBefore() {
        classifyTableContext = new NamingContext("classifyTableContext", "test-instance");

        final Optional<Mapping> ifcMapping = ContextTestUtils.getMapping(TABLE_NAME, TABLE_INDEX);
        doReturn(ifcMapping).when(mappingContext).read(any());
    }

    @Override
    protected ReaderCustomizer<ClassifySession, ClassifySessionBuilder> initCustomizer() {
        return new ClassifySessionReader(api, classifyTableContext);
    }

    private static InstanceIdentifier<ClassifySession> getClassifySessionId(final String tableName,
                                                                            final String match) {
        return InstanceIdentifier.create(VppClassifierState.class)
            .child(ClassifyTable.class, new ClassifyTableKey(tableName))
            .child(ClassifySession.class, new ClassifySessionKey(new HexString(match)));
    }

    @Test
    public void testMerge() {
        final ClassifyTableBuilder builder = mock(ClassifyTableBuilder.class);
        final List<ClassifySession> value = mock(List.class);
        getCustomizer().merge(builder, value);
        verify(builder).setClassifySession(value);
    }

    @Test
    public void testReadWithCache() throws ReadFailedException {
        final InstanceIdentifier<ClassifySession> id = getClassifySessionId(TABLE_NAME, MATCH_1);
        final ClassifySessionBuilder builder = mock(ClassifySessionBuilder.class);
        final ModificationCache cache = new ModificationCache();
        final ClassifySessionDetailsReplyDump dump = new ClassifySessionDetailsReplyDump();
        final ClassifySessionDetails details = new ClassifySessionDetails();
        details.match =
            new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                (byte) 0x05, (byte) 0x06, 0x00, 0x00, 0x00, 0x00};
        dump.classifySessionDetails = Collections.singletonList(details);
        cache.put(ClassifySessionReader.CACHE_KEY + id.firstKeyOf(ClassifyTable.class), dump);
        when(ctx.getModificationCache()).thenReturn(cache);

        getCustomizer().readCurrentAttributes(id, builder, ctx);
    }

    @Test
    public void testGetAllIds() throws ReadFailedException {
        final InstanceIdentifier<ClassifySession> id = getClassifySessionId(TABLE_NAME, MATCH_1);
        final ClassifySessionDetailsReplyDump dump = new ClassifySessionDetailsReplyDump();
        final ClassifySessionDetails details1 = new ClassifySessionDetails();
        details1.match =
            new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                (byte) 0x05, (byte) 0x06, 0x00, 0x00, 0x00, 0x00};
        final ClassifySessionDetails details2 = new ClassifySessionDetails();
        details2.match =
            new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                (byte) 0x05, (byte) 0x07, 0x00, 0x00, 0x00, 0x00};
        dump.classifySessionDetails = Arrays.asList(details1, details2);

        final CompletableFuture<ClassifySessionDetailsReplyDump> replyFuture = new CompletableFuture<>();
        replyFuture.complete(dump);
        doReturn(replyFuture).when(api).classifySessionDump(any(ClassifySessionDump.class));

        final List<ClassifySessionKey> allIds = getCustomizer().getAllIds(id, ctx);
        assertEquals(2, allIds.size());
        assertEquals(MATCH_1, allIds.get(0).getMatch().getValue());
        assertEquals(MATCH_2, allIds.get(1).getMatch().getValue());
    }

}