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
import io.fd.honeycomb.v3po.impl.trans.SubtreeManager;
import io.fd.honeycomb.v3po.impl.trans.VppException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Base VPP writer, responsible for translation between DataObjects and VPP APIs. Handling all update operations(create,
 * update, delete)
 *
 * @param <D> Specific DataObject derived type, that is handled by this writer
 */
@Beta
public interface VppWriter<D extends DataObject> extends SubtreeManager<D> {

    /**
     * Handle update operation. U from CRUD.
     *
     * @param id         Identifier(from root) of data being written
     * @param dataBefore Old data
     * @param dataAfter  New, updated data
     * @param ctx        Write context enabling writer to get information about candidate data as well as current data
     * @throws VppException if update failed
     */
    void update(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                @Nullable final DataObject dataBefore,
                @Nullable final DataObject dataAfter,
                @Nonnull final WriteContext ctx) throws VppException;
}
