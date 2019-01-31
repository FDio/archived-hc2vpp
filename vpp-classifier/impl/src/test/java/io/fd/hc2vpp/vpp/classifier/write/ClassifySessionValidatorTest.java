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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.classify.table.base.attributes.ClassifySession;

public class ClassifySessionValidatorTest {

    @Mock
    private WriteContext writeContext;
    @Mock
    private VppClassifierContextManager classifyTableContext;
    @Mock
    private ClassifySession session;

    private NamingContext policerContext;
    private ClassifySessionValidator validator;

    private static final String MATCH = "00:00:00:00:00:00:01:02:03:04:05:06:00:00:00:00";
    private static final String TABLE_NAME = "table123";

    @Before
    public void setUp() {
        initMocks(this);
        policerContext = new NamingContext("testPolicerContext", "testPolicerContext");
        validator = new ClassifySessionValidator(classifyTableContext, policerContext);
    }

    @Test
    public void testWriteSuccessfull()
            throws CreateValidationFailedException {
        when(classifyTableContext.containsTable(eq(TABLE_NAME), any())).thenReturn(true);
        validator.validateWrite(ClassifySessionWriterTest.getClassifySessionId(TABLE_NAME, MATCH), session,
                writeContext);
    }

    @Test(expected = DeleteValidationFailedException.class)
    public void testDeleteFailedContextMissingTable()
            throws DeleteValidationFailedException {
        when(classifyTableContext.containsTable(eq(TABLE_NAME), any())).thenReturn(Boolean.FALSE);
        validator.validateDelete(ClassifySessionWriterTest.getClassifySessionId(TABLE_NAME, MATCH), session,
                writeContext);
    }
}
