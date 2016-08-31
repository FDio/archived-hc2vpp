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

package io.fd.honeycomb.lisp.translate.read.dump.check;


import io.fd.honeycomb.translate.v3po.util.cache.EntityDumpNonEmptyCheck;
import io.fd.honeycomb.translate.v3po.util.cache.exceptions.check.DumpCheckFailedException;
import io.fd.honeycomb.translate.v3po.util.cache.exceptions.check.i.DumpEmptyException;
import org.openvpp.jvpp.core.dto.LispEidTableMapDetailsReplyDump;

public class VniTableDumpCheck implements EntityDumpNonEmptyCheck<LispEidTableMapDetailsReplyDump> {

    @Override
    public void assertNotEmpty(final LispEidTableMapDetailsReplyDump data) throws DumpCheckFailedException {
        if (data == null) {
            throw new DumpEmptyException("Vni table dump is null");
        }

        if (data.lispEidTableMapDetails == null) {
            throw new DumpEmptyException("Bni table dump is empty");
        }
    }
}
