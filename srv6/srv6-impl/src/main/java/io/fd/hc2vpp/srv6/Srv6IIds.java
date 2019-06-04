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


package io.fd.hc2vpp.srv6;

import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ietf.srv6.base.rev180613.VppSrv6FibLocatorAugment;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ietf.srv6.base.rev180613.vpp.srv6.fib.FibTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.Locator1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.multi.paths.v6.Paths;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.multi.paths.v6.paths.Path;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.routing.srv6.locators.locator.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.routing.srv6.locators.locator._static.LocalSids;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.Sid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.End;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndB6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndB6Encaps;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndBm;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDt4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDt46;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDt6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDx2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDx4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDx6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndT;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndX;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.Routing1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.routing.Srv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.encap.Encapsulation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.Locators;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.Locator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.locator.Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Srv6IIds {
    public static final InstanceIdentifier<Routing> RT = InstanceIdentifier.create(Routing.class);
    public static final InstanceIdentifier<Routing1> RT_RT1_AUG = RT.augmentation(Routing1.class);
    public static final InstanceIdentifier<Srv6> RT_SRV6 = RT_RT1_AUG.child(Srv6.class);
    public static final InstanceIdentifier<Locators> RT_SRV6_LOCATORS = RT_SRV6.child(Locators.class);
    public static final InstanceIdentifier<Locator> RT_SRV6_LOCS_LOCATOR = RT_SRV6_LOCATORS.child(Locator.class);
    public static final InstanceIdentifier<Locator> LOCATOR = InstanceIdentifier.create(Locator.class);
    public static final InstanceIdentifier<VppSrv6FibLocatorAugment> LOC_FT_AUG =
            LOCATOR.augmentation(VppSrv6FibLocatorAugment.class);
    public static final InstanceIdentifier<FibTable> LOC_FT = LOC_FT_AUG.child(FibTable.class);

    public static final InstanceIdentifier<Prefix> LOC_PREFIX = LOCATOR.child(Prefix.class);
    public static final InstanceIdentifier<Locator1> RT_SRV6_LOCS_LOC_AUG =
            RT_SRV6_LOCS_LOCATOR.augmentation(Locator1.class);
    public static final InstanceIdentifier<Static> RT_SRV6_LOCS_LOC_STATIC = RT_SRV6_LOCS_LOC_AUG.child(Static.class);
    public static final InstanceIdentifier<LocalSids> RT_SRV6_LOCS_LOC_ST_LOCALSIDS =
            RT_SRV6_LOCS_LOC_STATIC.child(LocalSids.class);
    public static final InstanceIdentifier<Sid> RT_SRV6_LOCS_LOC_ST_LS_SID =
            RT_SRV6_LOCS_LOC_ST_LOCALSIDS.child(Sid.class);
    public static final InstanceIdentifier<Encapsulation> RT_SRV6_ENCAP = RT_SRV6.child(Encapsulation.class);

    public static final InstanceIdentifier<Sid> SID = InstanceIdentifier.create(Sid.class);
    public static final InstanceIdentifier<End> SID_END = SID.child(End.class);
    public static final InstanceIdentifier<EndX> SID_END_X = SID.child(EndX.class);
    public static final InstanceIdentifier<Paths> SID_END_X_PATHS = SID_END_X.child(Paths.class);
    public static final InstanceIdentifier<Path> SID_END_X_PATHS_PATH = SID_END_X_PATHS.child(Path.class);
    public static final InstanceIdentifier<EndT> SID_END_T = SID.child(EndT.class);
    public static final InstanceIdentifier<EndB6> SID_END_B6 = SID.child(EndB6.class);
    public static final InstanceIdentifier<EndB6Encaps> SID_END_B6ENCAP = SID.child(EndB6Encaps.class);
    public static final InstanceIdentifier<EndBm> SID_END_BM = SID.child(EndBm.class);
    public static final InstanceIdentifier<EndDt4> SID_END_DT4 = SID.child(EndDt4.class);
    public static final InstanceIdentifier<EndDt6> SID_END_DT6 = SID.child(EndDt6.class);
    public static final InstanceIdentifier<EndDt46> SID_END_DT46 = SID.child(EndDt46.class);
    public static final InstanceIdentifier<EndDx2> SID_END_DX2 = SID.child(EndDx2.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.end.dx2.Paths>
            SID_END_DX2_PATHS = SID_END_DX2.child(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.end.dx2.Paths.class);
    public static final InstanceIdentifier<EndDx4> SID_END_DX4 = SID.child(EndDx4.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.multi.paths.v4.Paths>
            SID_END_DX4_PATHS = SID_END_DX4.child(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.multi.paths.v4.Paths.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.multi.paths.v4.paths.Path>
            SID_END_DX4_PATHS_PATH = SID_END_DX4_PATHS.child(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.multi.paths.v4.paths.Path.class);
    public static final InstanceIdentifier<EndDx6> SID_END_DX6 = SID.child(EndDx6.class);
    public static final InstanceIdentifier<Paths> SID_END_DX6_PATHS = SID_END_DX6.child(Paths.class);
    public static final InstanceIdentifier<Path> SID_END_DX6_PATHS_PATH = SID_END_DX6_PATHS.child(Path.class);
}
