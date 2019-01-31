/*
 * Copyright (c) 2019 PANTHEON.tech.
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

package io.fd.hc2vpp.vpp.classifier.write;

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.vpp.classifier.ClassifyTable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ClassifyTableValidator implements Validator<ClassifyTable>, ClassifyWriter {

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<ClassifyTable> id,
                              @Nonnull final ClassifyTable table,
                              @Nonnull final WriteContext writeContext)
            throws CreateValidationFailedException {
        try {
            validateTable(id, table);
        } catch (RuntimeException e) {
            throw new CreateValidationFailedException(id, table, e);
        }
    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<ClassifyTable> id,
                               @Nonnull final ClassifyTable dataBefore,
                               @Nonnull final WriteContext writeContext)
            throws DeleteValidationFailedException {
        try {
            validateTable(id, dataBefore);
        } catch (RuntimeException e) {
            throw new DeleteValidationFailedException(id, e);
        }
    }

    private void validateTable(final InstanceIdentifier<ClassifyTable> id, final ClassifyTable table) {
        checkArgument(table.getNbuckets() != null, "nbuckets is a mandatory field and is missing");
        checkArgument(table.getMemorySize() != null, "memorySize is a mandatory field and is missing");
        checkArgument(table.getSkipNVectors() != null, "skipNVectors is a mandatory field and is missing");
        checkArgument(table.getSkipNVectors() != null, "skipNVectors is a mandatory field and is missing");
        if (table.getMask() != null) {
            checkArgument(getBinaryVector(table.getMask()).length % 16 == 0,
                    "Number of mask bytes must be multiple of 16.");
        }
    }
}
