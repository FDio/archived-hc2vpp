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
import static java.nio.charset.StandardCharsets.UTF_8;

import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.TimeoutException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.pitr.cfg.grouping.PitrCfg;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.LispPitrSetLocatorSet;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;


/**
 * Customizer for {@code PitrCfg}
 */
public class PitrCfgCustomizer extends FutureJVppCustomizer
        implements WriterCustomizer<PitrCfg>, JvppReplyConsumer, ByteDataTranslator {

    private static final String DEFAULT_LOCATOR_SET_NAME = "N/A";

    public PitrCfgCustomizer(FutureJVppCore futureJvpp) {
        super(futureJvpp);
    }

    @Override
    public void writeCurrentAttributes(InstanceIdentifier<PitrCfg> id, PitrCfg dataAfter, WriteContext writeContext)
            throws WriteFailedException {
        checkNotNull(dataAfter, "PitrCfg is null");
        checkNotNull(dataAfter.getLocatorSet(), "Locator set name is null");

        try {
            addDelPitrSetLocatorSetAndReply(true, dataAfter);
        } catch (VppBaseCallException | TimeoutException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(InstanceIdentifier<PitrCfg> id, PitrCfg dataBefore, PitrCfg dataAfter,
                                        WriteContext writeContext) throws WriteFailedException {
        checkNotNull(dataAfter, "PitrCfg is null");
        checkNotNull(dataAfter.getLocatorSet(), "Locator set name is null");

        try {
            addDelPitrSetLocatorSetAndReply(true, dataAfter);
        } catch (VppBaseCallException | TimeoutException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(InstanceIdentifier<PitrCfg> id, PitrCfg dataBefore, WriteContext writeContext)
            throws WriteFailedException {
        checkNotNull(dataBefore, "PitrCfg is null");
        checkNotNull(dataBefore.getLocatorSet(), "Locator set name is null");

        try {
            addDelPitrSetLocatorSetAndReply(false, dataBefore);
        } catch (VppBaseCallException | TimeoutException e) {
            throw new WriteFailedException.CreateFailedException(id, dataBefore, e);
        }
    }

    private void addDelPitrSetLocatorSetAndReply(boolean add, PitrCfg data)
            throws VppBaseCallException, TimeoutException {

        if (DEFAULT_LOCATOR_SET_NAME.equals(data.getLocatorSet())) {
            // ignores attempts to write default locator set
            // therefore even while its loaded to config data of honeycomb while starting
            // you can still enable/disable Lisp without having to define N/A as default pitr-set
            return;
        }

        LispPitrSetLocatorSet request = new LispPitrSetLocatorSet();
        request.isAdd = booleanToByte(add);
        request.lsName = data.getLocatorSet().getBytes(UTF_8);

        getReply(getFutureJVpp().lispPitrSetLocatorSet(request).toCompletableFuture());
    }

}
