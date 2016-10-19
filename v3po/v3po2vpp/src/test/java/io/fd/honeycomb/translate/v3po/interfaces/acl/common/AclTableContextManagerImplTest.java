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

package io.fd.honeycomb.translate.v3po.interfaces.acl.common;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.honeycomb.translate.MappingContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.mapping.entry.context.attributes.acl.mapping.entry.context.MappingTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.mapping.entry.context.attributes.acl.mapping.entry.context.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.mapping.entry.context.attributes.acl.mapping.entry.context.mapping.table.MappingEntryBuilder;

public class AclTableContextManagerImplTest {

    private AclTableContextManagerImpl ctx;
    @Mock
    private MappingContext mappingContext;
    private static final int INDEX = 42;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ctx = new AclTableContextManagerImpl(MappingTable.Direction.Ingress);
    }

    @Test
    public void testAddEntry() throws Exception {
        final MappingEntry entry =
            new MappingEntryBuilder().setL2TableId(1).setIp4TableId(2).setIp6TableId(3).setIndex(INDEX).build();
        ctx.addEntry(entry, mappingContext);
        verify(mappingContext).put(ctx.getId(INDEX), entry);
    }

    @Test
    public void testRemoveEntry() throws Exception {
        ctx.removeEntry(INDEX, mappingContext);
        verify(mappingContext).delete(ctx.getId(INDEX));
    }

    @Test
    public void testReadEntry() throws Exception {
        ctx.getEntry(INDEX, mappingContext);
        verify(mappingContext).read(ctx.getId(INDEX));
    }
}