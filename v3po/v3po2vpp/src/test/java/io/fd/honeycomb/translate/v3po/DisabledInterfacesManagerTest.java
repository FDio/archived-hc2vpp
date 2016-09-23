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

package io.fd.honeycomb.translate.v3po;




import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.MappingContext;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.context.rev160909.DisabledInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.context.rev160909.DisabledInterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.context.rev160909.disabled.interfaces.DisabledInterfaceIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.context.rev160909.disabled.interfaces.DisabledInterfaceIndexBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.context.rev160909.disabled.interfaces.DisabledInterfaceIndexKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class DisabledInterfacesManagerTest {

    private static final InstanceIdentifier<DisabledInterfaces>
            ROOT_ID = InstanceIdentifier.create(DisabledInterfaces.class);
    private static final KeyedInstanceIdentifier<DisabledInterfaceIndex, DisabledInterfaceIndexKey> SPECIFIC_ID_1 =
            ROOT_ID.child(DisabledInterfaceIndex.class, new DisabledInterfaceIndexKey(1));
    private static final KeyedInstanceIdentifier<DisabledInterfaceIndex, DisabledInterfaceIndexKey> SPECIFIC_ID_4 =
            ROOT_ID.child(DisabledInterfaceIndex.class, new DisabledInterfaceIndexKey(4));

    @Mock
    private MappingContext mappingContext;
    private DisabledInterfacesManager manager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        manager = new DisabledInterfacesManager();
        doReturn(Optional.of(new DisabledInterfacesBuilder().setDisabledInterfaceIndex(toIndices(1, 2, 3)).build()))
                .when(mappingContext)
                .read(ROOT_ID);
        doReturn(Optional.of(toIndex(1)))
                .when(mappingContext)
                .read(SPECIFIC_ID_1);
        doReturn(Optional.absent())
                .when(mappingContext)
                .read(SPECIFIC_ID_4);
    }

    @Test
    public void testGetAll() throws Exception {
        final List<Integer> disabledInterfaces = manager.getDisabledInterfaces(mappingContext);
        assertThat(disabledInterfaces, hasItems(1, 2, 3));
    }

    @Test
    public void testCheckOne() throws Exception {
        assertTrue(manager.isInterfaceDisabled(1, mappingContext));
        assertFalse(manager.isInterfaceDisabled(4, mappingContext));
    }

    @Test
    public void testDisable() throws Exception {
        manager.disableInterface(1, mappingContext);
        verify(mappingContext).put(SPECIFIC_ID_1, toIndex(1));
    }

    @Test
    public void testRemoveDisability() throws Exception {
        manager.removeDisabledInterface(1, mappingContext);
        verify(mappingContext).delete(SPECIFIC_ID_1);
    }

    private List<DisabledInterfaceIndex> toIndices(final int... indices) {
        return Arrays.stream(indices)
                .mapToObj(this::toIndex)
                .collect(Collectors.toList());
    }

    private DisabledInterfaceIndex toIndex(final int idx) {
        return new DisabledInterfaceIndexBuilder()
                .setIndex(idx)
                .build();
    }
}