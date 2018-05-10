/*
 * Copyright (c) 2018 Bell Canada, Pantheon and/or its affiliates.
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

package io.fd.hc2vpp.routing.services;

import static java.lang.String.format;

import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface FibTableService {

    /**
     * Checks whether FIB table with provided index exist in VPP
     *
     * @throws ReadFailedException                           if there was an error while reading fib tables
     * @throws FibTableService.FibTableDoesNotExistException if requested index does not exist
     */
    void checkTableExist(@Nonnegative final int index, @Nonnull final ModificationCache cache)
            throws ReadFailedException, FibTableService.FibTableDoesNotExistException;

    /**
     * Writes FIB table in VPP
     *
     * @param identifier id of currently processed data
     * @param tableId    table Id to be written in VPP
     * @param tableName  name of the FIB table that will be added
     * @param isIpv6     true if adding IPv6 FIB table, false if adding IPv4 table
     * @throws WriteFailedException if there was an error while writing FIB tables
     */
    void write(InstanceIdentifier<?> identifier, @Nonnegative int tableId, @Nonnull String tableName, boolean isIpv6)
            throws WriteFailedException;

    class FibTableDoesNotExistException extends Exception {

        public FibTableDoesNotExistException(final int index) {
            super(format("Fib table with index %s does not exist", index));
        }
    }
}
