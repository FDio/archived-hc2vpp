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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.classify.table.base.attributes.ClassifySession;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.vpp.classifier.ClassifyTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.vpp.classifier.ClassifyTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ClassifySessionValidator implements Validator<ClassifySession>, ClassifyWriter{

    private final VppClassifierContextManager classifyTableContext;

    public ClassifySessionValidator(@Nonnull final VppClassifierContextManager classifyTableContext,
                                    @Nonnull final NamingContext policerContext) {
        this.classifyTableContext = checkNotNull(classifyTableContext, "classifyTableContext should not be null");
        checkNotNull(policerContext, "policerContext should not be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<ClassifySession> id,
                              @Nonnull final ClassifySession session,
                              @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.CreateValidationFailedException {
        try {
            validateSession(id, writeContext);
        } catch (RuntimeException e) {
            throw new DataValidationFailedException.CreateValidationFailedException(id, session, e);
        }
    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<ClassifySession> id,
                               @Nonnull final ClassifySession dataBefore,
                               @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.DeleteValidationFailedException {
        try {
            validateSession(id, writeContext);
        } catch (RuntimeException e) {
            throw new DataValidationFailedException.DeleteValidationFailedException(id, e);
        }
    }

    private void validateSession(final InstanceIdentifier<ClassifySession> id,
                                 @Nonnull final WriteContext writeContext) {
        final ClassifyTableKey tableKey = id.firstKeyOf(ClassifyTable.class);
        Preconditions.checkArgument(tableKey != null, "could not find classify table key in {}", id);
        final String tableName = tableKey.getName();
        Preconditions.checkState(classifyTableContext.containsTable(tableName, writeContext.getMappingContext()),
                "Could not find classify table index for {} in the classify table context", tableName);
    }
}
