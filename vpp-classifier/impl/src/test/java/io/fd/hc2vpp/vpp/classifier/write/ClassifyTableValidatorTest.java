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

import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.PacketHandlingAction;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.VppNode;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.VppNodeName;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.vpp.classifier.ClassifyTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.vpp.classifier.ClassifyTableBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.vpp.classifier.ClassifyTableKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.HexString;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ClassifyTableValidatorTest implements ByteDataTranslator {

    private static final String TABLE_NAME = "table123";
    private InstanceIdentifier<ClassifyTable> tableIID;

    @Mock
    private WriteContext writeContext;

    private ClassifyTableValidator validator;

    @Before
    public void setUp() {
        initMocks(this);
        validator = new ClassifyTableValidator();
        tableIID = ClassifyTableWriterTest.getClassifyTableId(TABLE_NAME);
    }

    @Test
    public void testWriteSuccessfull()
            throws CreateValidationFailedException {
        ClassifyTableBuilder builder = generatePrePopulatedClassifyTableBuilder(TABLE_NAME);
        validator.validateWrite(tableIID, builder.build(), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedOnEmptyNBuckets()
            throws CreateValidationFailedException {
        ClassifyTableBuilder builder = generatePrePopulatedClassifyTableBuilder(TABLE_NAME);
        builder.setNbuckets(null);
        validator.validateWrite(tableIID, builder.build(), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedOnEmptyMemorySize()
            throws CreateValidationFailedException {
        ClassifyTableBuilder builder = generatePrePopulatedClassifyTableBuilder(TABLE_NAME);
        builder.setMemorySize(null);
        validator.validateWrite(tableIID, builder.build(), writeContext);
    }

    @Test(expected = DeleteValidationFailedException.class)
    public void testDeleteFailedOnEmptySkipNVectors()
            throws DeleteValidationFailedException {
        ClassifyTableBuilder builder = generatePrePopulatedClassifyTableBuilder(TABLE_NAME);
        builder.setSkipNVectors(null);
        validator.validateDelete(tableIID, builder.build(), writeContext);
    }

    private ClassifyTableBuilder generatePrePopulatedClassifyTableBuilder(final String name) {
        final ClassifyTableBuilder builder = new ClassifyTableBuilder();
        builder.setName(name);
        builder.setClassifierNode(new VppNodeName("ip4-classifier"));
        builder.withKey(new ClassifyTableKey(name));
        builder.setSkipNVectors(0L);
        builder.setNbuckets(2L);
        builder.setMemorySize(2L << 20);
        builder.setMissNext(new VppNode(PacketHandlingAction.Permit));
        builder.setMask(new HexString("00:00:00:00:00:00:01:02:03:04:05:06:00:00:00:00"));
        return builder;
    }
}
