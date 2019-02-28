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

package io.fd.hc2vpp.lisp.translate.read.trait;


import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.SubtableDumpParams.MapLevel;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.jvpp.core.dto.OneEidTableMapDetails;
import io.fd.jvpp.core.dto.OneEidTableMapDetailsReplyDump;
import io.fd.jvpp.core.dto.OneEidTableMapDump;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;

public abstract class SubtableReaderTestCase<D extends DataObject, B extends Builder<D>>
        extends ReaderCustomizerTest<D, B>
        implements SubtableReader {

    protected final long expectedVni = 12;
    protected final int expectedTableId = 14;

    @Captor
    protected ArgumentCaptor<OneEidTableMapDump> requestCaptor;

    public SubtableReaderTestCase(final Class<D> dataObjectClass,
                                  final Class<? extends Builder<? extends DataObject>> parentBuilderClass) {
        super(dataObjectClass, parentBuilderClass);
    }

    protected void doReturnValidNonEmptyDataOnDump() {
        OneEidTableMapDetailsReplyDump reply = new OneEidTableMapDetailsReplyDump();
        OneEidTableMapDetails detailFirst = new OneEidTableMapDetails();
        detailFirst.vni = Long.valueOf(expectedVni).intValue();
        detailFirst.dpTable = expectedTableId;

        OneEidTableMapDetails detailSecond = new OneEidTableMapDetails();
        detailSecond.vni = 13;
        detailSecond.dpTable = 15;

        reply.oneEidTableMapDetails = ImmutableList.of(detailFirst, detailSecond);

        when(api.oneEidTableMapDump(any(OneEidTableMapDump.class)))
                .thenReturn(future(reply));
    }

    protected void doReturnEmptyDataOnDump() {
        OneEidTableMapDetailsReplyDump reply = new OneEidTableMapDetailsReplyDump();
        reply.oneEidTableMapDetails = Collections.emptyList();
        when(api.oneEidTableMapDump(any(OneEidTableMapDump.class)))
                .thenReturn(future(reply));
    }

    protected void doThrowOnDump() {
        when(api.oneEidTableMapDump(any(OneEidTableMapDump.class)))
                .thenReturn(failedFuture());
    }

    protected void verifyOneEidTableMapDumpCalled(@Nonnull final MapLevel expectedLevel) {
        verify(api, times(1)).oneEidTableMapDump(requestCaptor.capture());
        assertEquals(expectedLevel.getValue(), requestCaptor.getValue().isL2);
    }

    protected void verifyOneEidTableMapDumpNotCalled() {
        verify(api, times(1)).oneEidTableMapDump(any());
    }
}
