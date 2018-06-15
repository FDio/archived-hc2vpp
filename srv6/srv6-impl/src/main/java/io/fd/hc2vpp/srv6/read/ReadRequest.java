/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.srv6.read;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Represents common interface for read requests
 *
 * @param <T> type class to be read
 * @param <K> key class of read type
 * @param <B> builder class for read type
 */
public interface ReadRequest<T extends DataObject & Identifiable<K>, K extends Identifier<T>, B extends Builder<T>> {

    /**
     * Provide list of keys for specified type
     *
     * @param identifier identifies path to DataObject class of specified type
     * @param ctx        read context holds modification cache and mapping context
     * @return list of keys for specified type
     * @throws ReadFailedException when read error occurs
     */
    @Nonnull
    List<K> readAllKeys(@Nonnull final InstanceIdentifier<T> identifier, @Nonnull final ReadContext ctx) throws
            ReadFailedException;

    /**
     * Reads one specific value
     *
     * @param identifier identifies path to DataObject class of specified type
     * @param ctx        read context holds modification cache and mapping context
     * @param builder    builder for particular type which will hold actual data
     * @throws ReadFailedException when read error occurs
     */
    void readSpecific(@Nonnull final InstanceIdentifier<T> identifier, @Nonnull final ReadContext ctx,
                      @Nonnull B builder)
            throws ReadFailedException;
}
