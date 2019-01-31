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

package io.fd.hc2vpp.policer.write;

import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.DscpType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionDrop;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionMarkDscp;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionTransmit;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ConformAction;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ConformActionBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ExceedAction;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ExceedActionBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ViolateAction;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ViolateActionBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policers.Policer;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policers.PolicerBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Dscp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PolicerValidatorTest {

    @Mock
    private WriteContext writeContext;

    private static final InstanceIdentifier<Policer> POLICER_IID = InstanceIdentifier.create(Policer.class);
    private static short DSCP = 10;
    private PolicerValidator validator;

    @Before
    public void setUp() {
        initMocks(this);
        validator = new PolicerValidator(new NamingContext("testPolicerValidator", "testPolicerValidator"));
    }

    @Test
    public void testWriteSuccessfull()
            throws CreateValidationFailedException {
        PolicerBuilder builder = generatePrePopulatedPolicerBuilder();
        validator.validateWrite(POLICER_IID, builder.build(), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedDscp()
            throws CreateValidationFailedException {
        PolicerBuilder builder = generatePrePopulatedPolicerBuilder();
        builder.setConformAction(generateConformAction(true, MeterActionDrop.class));
        validator.validateWrite(POLICER_IID, builder.build(), writeContext);
    }

    @Test(expected = DeleteValidationFailedException.class)
    public void testDeleteFailedDscp()
            throws DeleteValidationFailedException {
        PolicerBuilder builder = generatePrePopulatedPolicerBuilder();
        builder.setExceedAction(generateExceedAction(true, MeterActionTransmit.class));
        validator.validateDelete(POLICER_IID, builder.build(), writeContext);
    }

    private PolicerBuilder generatePrePopulatedPolicerBuilder() {
        PolicerBuilder builder = new PolicerBuilder();
        builder.setConformAction(generateConformAction(true, MeterActionMarkDscp.class))
                .setExceedAction(generateExceedAction(false, MeterActionTransmit.class))
                .setViolateAction(generateViolateAction(false, MeterActionDrop.class));
        return builder;
    }

    private ExceedAction generateExceedAction(final boolean hasDscp,
                                              final Class<? extends MeterActionType> actionType) {
        ExceedActionBuilder builder = new ExceedActionBuilder();
        if (hasDscp) {
            builder.setDscp(new DscpType(new Dscp(DSCP)));
        }
        builder.setMeterActionType(actionType);
        return builder.build();
    }

    private ConformAction generateConformAction(final boolean hasDscp,
                                                final Class<? extends MeterActionType> actionType) {
        ConformActionBuilder builder = new ConformActionBuilder();
        if (hasDscp) {
            builder.setDscp(new DscpType(new Dscp(DSCP)));
        }
        builder.setMeterActionType(actionType);
        return builder.build();
    }

    private ViolateAction generateViolateAction(final boolean hasDscp,
                                                final Class<? extends MeterActionType> actionType) {
        ViolateActionBuilder builder = new ViolateActionBuilder();
        if (hasDscp) {
            builder.setDscp(new DscpType(new Dscp(DSCP)));
        }
        builder.setMeterActionType(actionType);
        return builder.build();
    }
}
