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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import io.fd.honeycomb.lisp.translate.write.trait.SubtableWriter;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeDomainSubtableCustomizer extends FutureJVppCustomizer
        implements WriterCustomizer<BridgeDomainSubtable>, SubtableWriter {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomainSubtableCustomizer.class);

    private final NamingContext bridgeDomainContext;

    public BridgeDomainSubtableCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                                          @Nonnull final NamingContext bridgeDomainContext) {
        super(futureJvpp);
        this.bridgeDomainContext = checkNotNull(bridgeDomainContext, "Bridge domain context cannot be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomainSubtable> id,
                                       @Nonnull final BridgeDomainSubtable dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Writing Id {} ", id);

        try {
            addDelSubtableMapping(getFutureJVpp(), true, extractVni(id),
                    extractBridgeDomainId(dataAfter.getBridgeDomainRef(), writeContext.getMappingContext()), true, LOG);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }

        LOG.debug("{} successfully written", id);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomainSubtable> id,
                                        @Nonnull final BridgeDomainSubtable dataBefore,
                                        @Nonnull final BridgeDomainSubtable dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomainSubtable> id,
                                        @Nonnull final BridgeDomainSubtable dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Removing Id {}", id);

        try {
            addDelSubtableMapping(getFutureJVpp(), false, extractVni(id),
                    extractBridgeDomainId(dataBefore.getBridgeDomainRef(), writeContext.getMappingContext()), true,
                    LOG);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataBefore, e);
        }

        LOG.debug("{} successfully removed", id);
    }

    private int extractBridgeDomainId(final String bridgeDomainName, final MappingContext mappingContext) {
        checkState(bridgeDomainContext.containsIndex(bridgeDomainName, mappingContext),
                "No mapping for bridge domain name %s", bridgeDomainName);
        return bridgeDomainContext.getIndex(bridgeDomainName, mappingContext);
    }
}
