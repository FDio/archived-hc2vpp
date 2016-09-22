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

package io.fd.honeycomb.translate.v3po.interfaces.ip.subnet.validation;


import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.NetmaskBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad;

public class SubnetValidatorTest {

    private SubnetValidator subnetValidator;

    @Before
    public void init() {
        subnetValidator = new SubnetValidator();
    }

    @Test(expected = SubnetValidationException.class)
    public void testValidateNegativeSameTypes() throws SubnetValidationException {
        List<Address> addresses = Lists.newArrayList();

        addresses.add(new AddressBuilder().setSubnet(new PrefixLengthBuilder().setPrefixLength((short) 24).build())
                .build());
        addresses.add(new AddressBuilder().setSubnet(new PrefixLengthBuilder().setPrefixLength((short) 24).build())
                .build());

        subnetValidator.checkNotAddingToSameSubnet(addresses);
    }

    @Test(expected = SubnetValidationException.class)
    public void testValidateNegativeMixedTypes() throws SubnetValidationException {
        List<Address> addresses = Lists.newArrayList();

        addresses.add(new AddressBuilder().setSubnet(new PrefixLengthBuilder().setPrefixLength((short) 24).build())
                .build());
        addresses.add(new AddressBuilder()
                .setSubnet(new NetmaskBuilder().setNetmask(new DottedQuad("255.255.255.0")).build())
                .build());

        subnetValidator.checkNotAddingToSameSubnet(addresses);
    }

    @Test
    public void testValidatePositiveSameTypes() throws SubnetValidationException {
        List<Address> addresses = Lists.newArrayList();

        addresses.add(new AddressBuilder().setSubnet(new PrefixLengthBuilder().setPrefixLength((short) 24).build())
                .build());
        addresses.add(new AddressBuilder().setSubnet(new PrefixLengthBuilder().setPrefixLength((short) 25).build())
                .build());

        subnetValidator.checkNotAddingToSameSubnet(addresses);
    }

    @Test
    public void testValidatePositiveMixedTypes() throws SubnetValidationException {
        List<Address> addresses = Lists.newArrayList();

        addresses.add(new AddressBuilder().setSubnet(new PrefixLengthBuilder().setPrefixLength((short) 24).build())
                .build());
        addresses.add(new AddressBuilder()
                .setSubnet(new NetmaskBuilder().setNetmask(new DottedQuad("255.255.0.0")).build())
                .build());

        subnetValidator.checkNotAddingToSameSubnet(addresses);
    }
}
