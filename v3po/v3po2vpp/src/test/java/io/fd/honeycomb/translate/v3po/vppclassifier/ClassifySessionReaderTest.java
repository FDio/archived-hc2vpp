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
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.HexString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.VppClassifierState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.classify.table.base.attributes.ClassifySession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.classify.table.base.attributes.ClassifySessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.classify.table.base.attributes.ClassifySessionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.vpp.classifier.state.ClassifyTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.vpp.classifier.state.ClassifyTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.vpp.classifier.state.ClassifyTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.ClassifySessionDetails;
import io.fd.vpp.jvpp.core.dto.ClassifySessionDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.ClassifySessionDump;

public class ClassifySessionReaderTest extends
    ListReaderCustomizerTest<ClassifySession, ClassifySessionKey, ClassifySessionBuilder> {

    private static final String MATCH_1 = "00:00:00:00:00:00:01:02:03:04:05:06:00:00:00:00";
    private static final String MATCH_2 = "00:00:00:00:00:00:01:02:03:04:05:07:00:00:00:00";

    private static final int TABLE_INDEX = 1;
    private static final String TABLE_NAME = "table1";

    @Mock
    private VppClassifierContextManager classifierContext;

    public ClassifySessionReaderTest() {
        super(ClassifySession.class, ClassifyTableBuilder.class);
    }

    @Override
    protected ReaderCustomizer<ClassifySession, ClassifySessionBuilder> initCustomizer() {
        return new ClassifySessionReader(api, classifierContext);
    }

    private static InstanceIdentifier<ClassifySession> getClassifySessionId(final String tableName,
                                                                            final String match) {
        return InstanceIdentifier.create(VppClassifierState.class)
            .child(ClassifyTable.class, new ClassifyTableKey(tableName))
            .child(ClassifySession.class, new ClassifySessionKey(new HexString(match)));
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
        doReturn(future(dump)).when(api).classifySessionDump(any(ClassifySessionDump.class));

        when(classifierContext.containsTable(TABLE_NAME, mappingContext)).thenReturn(true);
        when(classifierContext.getTableIndex(TABLE_NAME, mappingContext)).thenReturn(TABLE_INDEX);

        final List<ClassifySessionKey> allIds = getCustomizer().getAllIds(id, ctx);
        assertEquals(2, allIds.size());
        assertEquals(MATCH_1, allIds.get(0).getMatch().getValue());
        assertEquals(MATCH_2, allIds.get(1).getMatch().getValue());
    }

}