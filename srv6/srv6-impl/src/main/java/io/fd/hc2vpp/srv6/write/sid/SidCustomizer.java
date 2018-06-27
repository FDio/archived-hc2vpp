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

package io.fd.hc2vpp.srv6.write.sid;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.googlecode.ipv6.IPv6NetworkMask;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionWriteBindingRegistry;
import io.fd.hc2vpp.srv6.write.sid.request.LocalSidFunctionRequest;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.ietf.srv6.base.rev180613.VppSrv6FibLocatorAugment;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.ietf.srv6.base.rev180613.vpp.srv6.fib.FibTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.Sid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.SidKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.Locator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.locator.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class SidCustomizer extends FutureJVppCustomizer implements ListWriterCustomizer<Sid, SidKey> {

    private final LocalSidFunctionWriteBindingRegistry bindingRegistry;

    public SidCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                         @Nonnull final LocalSidFunctionWriteBindingRegistry bindingRegistry) {
        super(futureJVppCore);
        this.bindingRegistry = bindingRegistry;
    }

    Ipv6Address resolveSidAddress(@Nonnull final Prefix locPrefix, @Nonnull Sid localSid) {
        com.googlecode.ipv6.IPv6Address ip =
                com.googlecode.ipv6.IPv6Address.fromString(locPrefix.getAddress().getValue());
        IPv6NetworkMask mask = IPv6NetworkMask.fromPrefixLength(locPrefix.getLength().getValue());
        // strip function part if present
        ip = ip.maskWithNetworkMask(mask);
        //add new function part based on opcode
        String locIp = ip.add(localSid.getOpcode().getValue().intValue()).toString();
        return new Ipv6Address(locIp);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Sid> instanceIdentifier,
                                       @Nonnull final Sid localSid, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        InstanceIdentifier<Locator> locatorIid = RWUtils.cutId(instanceIdentifier, Locator.class);
        Optional<Locator> locatorOpt = writeContext.readAfter(locatorIid);
        Table vrfTable = getVrfTable(instanceIdentifier, writeContext, locatorIid, locatorOpt);
        LocalSidFunctionRequest request = bindRequest(extractLocPrefix(instanceIdentifier, locatorOpt, localSid),
                localSid, vrfTable.getTableId().getValue().intValue(), writeContext);
        request.write(instanceIdentifier);
    }

    private Table getVrfTable(final @Nonnull InstanceIdentifier<Sid> iid, final @Nonnull WriteContext writeContext,
                              final InstanceIdentifier<Locator> locatorIid, final Optional<Locator> locatorOpt) {
        Preconditions.checkArgument(locatorOpt.isPresent(), "Locator: {} for SID: {} was not found.", locatorIid, iid);
        Preconditions.checkNotNull(locatorOpt.get().getAugmentation(VppSrv6FibLocatorAugment.class),
                "Vpp FIB table augmentation was not found for SID: {}.", iid);
        FibTable fibTable = locatorOpt.get().getAugmentation(VppSrv6FibLocatorAugment.class).getFibTable();
        Preconditions.checkNotNull(fibTable, "Vpp FIB table configuration was not found for SID: {}.", iid);
        TableKey tableKey = new TableKey(fibTable.getAddressFamily(), fibTable.getTableId());
        KeyedInstanceIdentifier<Table, TableKey> vrfIid = FibManagementIIds.FM_FIB_TABLES.child(Table.class, tableKey);
        if (!writeContext.readAfter(vrfIid).isPresent()) {
            throw new IllegalArgumentException(
                    String.format("VRF table: %s not found. Create table before writing SID : %s.", tableKey, iid));
        }
        return writeContext.readAfter(vrfIid).get();
    }

    private Prefix extractLocPrefix(final @Nonnull InstanceIdentifier<Sid> instanceIdentifier,
                                    Optional<Locator> locatorOpt, final @Nonnull Sid localSid)
            throws WriteFailedException {
        Preconditions.checkArgument(locatorOpt.isPresent(), "Cannot read locator for sid: {}, with IId: ", localSid,
                instanceIdentifier);
        Locator loc = locatorOpt.get();
        if (loc.getPrefix() == null || loc.getPrefix().getAddress() == null || loc.getPrefix().getLength() == null) {
            throw new WriteFailedException(instanceIdentifier,
                    String.format("Cannot parse locator prefix for local sid %s", localSid));
        }
        return loc.getPrefix();
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Sid> instanceIdentifier,
                                        @Nonnull final Sid localSid, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        InstanceIdentifier<Locator> locatorIid = RWUtils.cutId(instanceIdentifier, Locator.class);
        Optional<Locator> locatorOpt = writeContext.readBefore(locatorIid);
        Table vrfTable = getVrfTable(instanceIdentifier, writeContext, locatorIid, locatorOpt);
        LocalSidFunctionRequest request = bindRequest(extractLocPrefix(instanceIdentifier, locatorOpt, localSid),
                localSid, vrfTable.getTableId().getValue().intValue(), writeContext);
        request.delete(instanceIdentifier);
    }

    private LocalSidFunctionRequest bindRequest(final @Nonnull Prefix locPrefix, final @Nonnull Sid localSid,
                                                final int installFibId, final @Nonnull WriteContext writeContext) {
        LocalSidFunctionRequest request = bindingRegistry.bind(localSid, writeContext);
        Ipv6Address sidAddress = resolveSidAddress(locPrefix, localSid);
        request.setLocalSidAddress(sidAddress);
        request.setInstallFibTable(installFibId);
        return request;
    }
}
