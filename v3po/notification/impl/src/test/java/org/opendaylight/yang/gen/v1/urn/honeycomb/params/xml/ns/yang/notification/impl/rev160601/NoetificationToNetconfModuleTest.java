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
package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.notification.impl.rev160601;

import javax.annotation.Nonnull;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.netconf.notifications.NetconfNotification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.*;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class NoetificationToNetconfModuleTest {

    private final DOMNotification notification =  new DOMNotification() {

        private QName qname = NetconfSessionStart.QNAME;
        private YangInstanceIdentifier.NodeIdentifier nodeIdentifier =
            new YangInstanceIdentifier.NodeIdentifier(NetconfSessionStart.QNAME);

        @Nonnull
        @Override
        public SchemaPath getType() {
            return SchemaPath.create(true, qname);
        }

        @Nonnull
        @Override
        public ContainerNode getBody() {
            return Builders.containerBuilder()
                .withNodeIdentifier(nodeIdentifier)
                .withChild(ImmutableNodes.leafNode(QName.create(qname, "username"), "user"))
                .withChild(ImmutableNodes.leafNode(QName.create(qname, "session-id"), 1))
                .withChild(ImmutableNodes.leafNode(QName.create(qname, "source-host"), "127.0.0.1"))
                .build();
        }
    };

    @Test
    public void notificationToXml() throws Exception {
        final ModuleInfoBackedContext moduleInfoBackedContext = getModuleInfoBackedCOntext();

        final NetconfNotification netconfNotification = HoneycombNotificationToNetconfTranslatorModule
            .notificationToXml(notification, moduleInfoBackedContext.getSchemaContext());

        final String notificationString = netconfNotification.toString();
        Assert.assertThat(notificationString, CoreMatchers.containsString("<netconf-session-start"));
        Assert.assertThat(notificationString, CoreMatchers.containsString("<username>user</username>"));
        Assert.assertThat(notificationString, CoreMatchers.containsString("eventTime"));
    }

    private static ModuleInfoBackedContext getModuleInfoBackedCOntext() {
        final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        final YangModuleInfo ietfNetconfNotifModuleInfo =
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.$YangModuleInfoImpl
                .getInstance();
        moduleInfoBackedContext.registerModuleInfo(ietfNetconfNotifModuleInfo);
        return moduleInfoBackedContext;
    }
}