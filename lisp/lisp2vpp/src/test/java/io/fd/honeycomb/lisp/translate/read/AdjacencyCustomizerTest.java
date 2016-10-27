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

package io.fd.honeycomb.lisp.translate.read;

import static org.junit.Assert.assertTrue;

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.AdjacenciesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.AdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.AdjacencyKey;
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