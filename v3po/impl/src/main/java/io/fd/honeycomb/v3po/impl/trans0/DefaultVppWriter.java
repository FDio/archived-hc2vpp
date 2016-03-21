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

package io.fd.honeycomb.v3po.impl.trans0;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultVppWriter implements VppWriter {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultVppWriter.class);

    @Override
    public void process(@Nullable final DataObject dataBefore, @Nullable final DataObject dataAfter)
            throws VppApiInvocationException {
        LOG.debug("Processing modification: dataBefore={}, dataAfter={}", dataBefore, dataAfter);

        if (Objects.equals(dataBefore, dataAfter)) {
            LOG.debug("No modification");
        } else if (dataBefore == null) {
            LOG.debug("modification type: CREATE");
        } else if (dataAfter == null) {
            LOG.debug("modification type: DELETE");
        } else {
            LOG.debug("modification type: UPDATE");
        }
    }
}
