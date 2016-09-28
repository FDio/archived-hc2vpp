package io.fd.honeycomb.lisp.translate.read;

import static org.junit.Assert.assertTrue;

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.AdjacenciesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.AdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.AdjacencyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class AdjacencyCustomizerTest extends ListReaderCustomizerTest<Adjacency, AdjacencyKey, AdjacencyBuilder> {

    private InstanceIdentifier<Adjacency> identifier;

    public AdjacencyCustomizerTest() {
        super(Adjacency.class, AdjacenciesBuilder.class);
    }

    @Before
    public void init() {
        identifier = InstanceIdentifier.create(Adjacency.class);
    }

    @Test
    public void getAllIds() throws Exception {
        assertTrue(getCustomizer().getAllIds(identifier, ctx).isEmpty());
    }

    @Test(expected = ReadFailedException.class)
    public void readCurrentAttributes() throws Exception {
        getCustomizer().readCurrentAttributes(identifier, new AdjacencyBuilder(), ctx);
    }

    @Override
    protected ReaderCustomizer<Adjacency, AdjacencyBuilder> initCustomizer() {
        return new AdjacencyCustomizer(api);
    }
}