package io.fd.honeycomb.v3po.impl.trans.w.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.impl.trans.util.Context;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class TransactionWriteContextTest {

    @Mock
    private BindingNormalizedNodeSerializer serializer;
    @Mock
    private DOMDataReadOnlyTransaction beforeTx;
    @Mock
    private DOMDataReadOnlyTransaction afterTx;
    @Mock
    private CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future;
    @Mock
    private Optional<org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode<?, ?>> optional;
    @Mock
    private Map.Entry entry;

    private TransactionWriteContext transactionWriteContext;

    @Before
    public void setUp() {
        initMocks(this);
        transactionWriteContext = new TransactionWriteContext(serializer, beforeTx, afterTx);
    }

    @Test
    public void testReadBeforeNoData() throws Exception {
        when(beforeTx.read(eq(LogicalDatastoreType.CONFIGURATION), any(YangInstanceIdentifier.class))).thenReturn(future);
        when(future.checkedGet()).thenReturn(optional);
        when(optional.isPresent()).thenReturn(false);

        final InstanceIdentifier<BridgeDomain> instanceId =
                InstanceIdentifier.create(Vpp.class).child(BridgeDomains.class).child(BridgeDomain.class);

        final Optional<DataObject> dataObjects = transactionWriteContext.readBefore(instanceId);
        assertNotNull(dataObjects);
        assertFalse(dataObjects.isPresent());

        verify(serializer).toYangInstanceIdentifier(instanceId);
        verify(serializer, never()).fromNormalizedNode(any(YangInstanceIdentifier.class), any(NormalizedNode.class));
    }


    @Test
    public void testReadBefore() throws Exception {
        when(beforeTx.read(eq(LogicalDatastoreType.CONFIGURATION), any(YangInstanceIdentifier.class))).thenReturn(future);
        when(future.checkedGet()).thenReturn(optional);
        when(optional.isPresent()).thenReturn(true);

        final InstanceIdentifier<BridgeDomain> instanceId =
                InstanceIdentifier.create(Vpp.class).child(BridgeDomains.class).child(BridgeDomain.class);
        final YangInstanceIdentifier yangId = YangInstanceIdentifier.builder().node(VppState.QNAME).node(
                BridgeDomains.QNAME).node(BridgeDomain.QNAME).build();
        when(serializer.toYangInstanceIdentifier(any(InstanceIdentifier.class))).thenReturn(yangId);
        when(serializer.fromNormalizedNode(eq(yangId), any(NormalizedNode.class))).thenReturn(entry);
        when(entry.getValue()).thenReturn(mock(DataObject.class));

        final Optional<DataObject> dataObjects = transactionWriteContext.readBefore(instanceId);
        assertNotNull(dataObjects);
        assertTrue(dataObjects.isPresent());

        verify(serializer).toYangInstanceIdentifier(instanceId);
        verify(serializer).fromNormalizedNode(eq(yangId), any(NormalizedNode.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testReadBeforeFailed() throws Exception {
        when(beforeTx.read(eq(LogicalDatastoreType.CONFIGURATION), any(YangInstanceIdentifier.class))).thenReturn(future);
        when(future.checkedGet()).thenThrow(ReadFailedException.class);
        transactionWriteContext.readBefore(mock(InstanceIdentifier.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testReadAfterFailed() throws Exception {
        when(afterTx.read(eq(LogicalDatastoreType.CONFIGURATION), any(YangInstanceIdentifier.class))).thenReturn(future);
        when(future.checkedGet()).thenThrow(ReadFailedException.class);
        transactionWriteContext.readAfter(mock(InstanceIdentifier.class));
    }

    @Test
    public void testGetContext() throws Exception {
        assertNotNull(transactionWriteContext.getContext());
    }

    @Test
    public void testClose() throws Exception {
        final Context context = transactionWriteContext.getContext();
        transactionWriteContext.close();
        // TODO verify context was closed
    }
}