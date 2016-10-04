package io.fd.honeycomb.lisp.translate.read.trait;


import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.SubtableDumpParams.MapLevel;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import io.fd.vpp.jvpp.VppCallbackException;
import io.fd.vpp.jvpp.core.dto.LispEidTableMapDetails;
import io.fd.vpp.jvpp.core.dto.LispEidTableMapDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.LispEidTableMapDump;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.junit.Before;
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
    protected ArgumentCaptor<LispEidTableMapDump> requestCaptor;

    public SubtableReaderTestCase(final Class<D> dataObjectClass,
                                  final Class<? extends Builder<? extends DataObject>> parentBuilderClass) {
        super(dataObjectClass, parentBuilderClass);
    }

    protected void doReturnValidNonEmptyDataOnDump() {
        LispEidTableMapDetailsReplyDump reply = new LispEidTableMapDetailsReplyDump();
        LispEidTableMapDetails detailFirst = new LispEidTableMapDetails();
        detailFirst.vni = Long.valueOf(expectedVni).intValue();
        detailFirst.dpTable = expectedTableId;

        LispEidTableMapDetails detailSecond = new LispEidTableMapDetails();
        detailSecond.vni = 13;
        detailSecond.dpTable = 15;

        reply.lispEidTableMapDetails = ImmutableList.of(detailFirst, detailSecond);

        when(api.lispEidTableMapDump(any(LispEidTableMapDump.class)))
                .thenReturn(future(reply));
    }

    protected void doReturnEmptyDataOnDump() {
        LispEidTableMapDetailsReplyDump reply = new LispEidTableMapDetailsReplyDump();
        reply.lispEidTableMapDetails = Collections.emptyList();
        when(api.lispEidTableMapDump(any(LispEidTableMapDump.class)))
                .thenReturn(future(reply));
    }

    protected void doThrowOnDump() {
        when(api.lispEidTableMapDump(any(LispEidTableMapDump.class)))
                .thenReturn(failedFuture());
    }

    protected void verifyLispEidTableMapDumpCalled(@Nonnull final MapLevel expectedLevel) {
        verify(api, times(1)).lispEidTableMapDump(requestCaptor.capture());
        assertEquals(expectedLevel.getValue(), requestCaptor.getValue().isL2);
    }

    protected void verifyLispEidTableMapDumpNotCalled() {
        verify(api, times(1)).lispEidTableMapDump(any());
    }
}
