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

package io.fd.honeycomb.v3po.impl.trans.w;

import com.google.common.annotations.Beta;
import io.fd.honeycomb.v3po.impl.trans.VppException;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Special {@link VppWriter} capable of performing bulk updates
 */
@Beta
public interface WriterRegistry extends VppWriter<DataObject> {

    /**
     * Performs bulk update
     *
     * @throws BulkUpdateException in case bulk update fails
     */
    void update(@Nonnull final Map<InstanceIdentifier<?>, DataObject> dataBefore,
                @Nonnull final Map<InstanceIdentifier<?>, DataObject> dataAfter,
                @Nonnull final WriteContext ctx) throws VppException, BulkUpdateException;

    @Beta
    public class BulkUpdateException extends VppException {

        private final Revert runnable;

        public BulkUpdateException(final InstanceIdentifier<?> id, final RuntimeException e, final Revert runnable) {
            super("Bulk edit failed at " + id, e);
            this.runnable = runnable;
        }

        public void revertChanges() throws VppException {
            runnable.revert();
        }
    }

    /**
     * Abstraction over revert mechanism in cast of a bulk update failure
     */
    @Beta
    public interface Revert {

        public void revert() throws VppException;
    }
}
