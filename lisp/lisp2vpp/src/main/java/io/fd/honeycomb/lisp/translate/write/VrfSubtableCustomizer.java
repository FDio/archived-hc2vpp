/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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

package io.fd.honeycomb.lisp.translate.write;

import io.fd.honeycomb.lisp.translate.write.trait.SubtableWriter;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VrfSubtableCustomizer extends FutureJVppCustomizer
        implements WriterCustomizer<VrfSubtable>, SubtableWriter {

    private static final Logger LOG = LoggerFactory.getLogger(VrfSubtableCustomizer.class);

    public VrfSubtableCustomizer(@Nonnull final FutureJVppCore futureJvpp) {
        super(futureJvpp);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<VrfSubtable> id,
                                       @Nonnull final VrfSubtable dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {

        LOG.debug("Writing Id[{}]/Data[{}]", id, dataAfter);

        try {
            addDelSubtableMapping(getFutureJVpp(), true, extractVni(id), dataAfter.getTableId().intValue(), false, LOG);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }

        LOG.debug("{} successfully written", id);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<VrfSubtable> id,
                                        @Nonnull final VrfSubtable dataBefore, @Nonnull final VrfSubtable dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<VrfSubtable> id,
                                        @Nonnull final VrfSubtable dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {

        LOG.debug("Removing Id[{}]/Data[{}]", id, dataBefore);

        try {
            addDelSubtableMapping(getFutureJVpp(), false, extractVni(id), dataBefore.getTableId().intValue(), false,
                    LOG);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataBefore, e);
        }

        LOG.debug("{} successfully removed", id);
    }

}
