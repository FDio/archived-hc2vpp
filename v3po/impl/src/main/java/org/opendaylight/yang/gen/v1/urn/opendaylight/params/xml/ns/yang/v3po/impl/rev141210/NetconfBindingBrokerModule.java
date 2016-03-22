package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.impl.rev141210;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.BundleContext;

public class NetconfBindingBrokerModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.impl.rev141210.AbstractNetconfBindingBrokerModule {
    public NetconfBindingBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfBindingBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.impl.rev141210.NetconfBindingBrokerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new FakeBindingAwareBroker(getNetconfBindingBrokerDependency());
    }

    private static class FakeBindingAwareBroker implements BindingAwareBroker, AutoCloseable {

        private DataBroker netconfBindingBrokerDependency;

        public FakeBindingAwareBroker(final DataBroker netconfBindingBrokerDependency) {

            this.netconfBindingBrokerDependency = netconfBindingBrokerDependency;
        }

        @Deprecated
        @Override
        public ConsumerContext registerConsumer(final BindingAwareConsumer bindingAwareConsumer,
                                                final BundleContext bundleContext) {
            throw new UnsupportedOperationException("Unsupported");
        }

        @Override
        public ConsumerContext registerConsumer(final BindingAwareConsumer bindingAwareConsumer) {
            final ConsumerContext consumerContext = new ConsumerContext() {
                @Override
                public <T extends BindingAwareService> T getSALService(final Class<T> aClass) {
                    return aClass.equals(DataBroker.class)
                        ? (T) netconfBindingBrokerDependency
                        : null;
                }

                @Override
                public <T extends RpcService> T getRpcService(final Class<T> aClass) {
                    return null;
                }
            };
            bindingAwareConsumer.onSessionInitialized(consumerContext);
            return consumerContext;
        }

        @Override
        public ProviderContext registerProvider(final BindingAwareProvider bindingAwareProvider,
                                                final BundleContext bundleContext) {
            throw new UnsupportedOperationException("Unsupported");
        }

        @Override
        public ProviderContext registerProvider(final BindingAwareProvider bindingAwareProvider) {
            final ProviderContext context = new ProviderContext() {
                @Override
                public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(
                    final L l) {
                    throw new UnsupportedOperationException("Unsupported");
                }

                @Override
                public <T extends RpcService> T getRpcService(final Class<T> aClass) {
                    throw new UnsupportedOperationException("Unsupported");
                }

                @Override
                public <T extends RpcService> RpcRegistration<T> addRpcImplementation(final Class<T> aClass, final T t)
                    throws IllegalStateException {
                    throw new UnsupportedOperationException("Unsupported");
                }

                @Override
                public <T extends RpcService> RoutedRpcRegistration<T> addRoutedRpcImplementation(
                    final Class<T> aClass, final T t) throws IllegalStateException {
                    throw new UnsupportedOperationException("Unsupported");
                }

                @Override
                public <T extends BindingAwareService> T getSALService(final Class<T> aClass) {
                    return aClass.equals(DataBroker.class)
                        ? (T) netconfBindingBrokerDependency
                        : null;                }
            };
            bindingAwareProvider.onSessionInitiated(context);
            return context;
        }

        @Override
        public void close() throws Exception {
            netconfBindingBrokerDependency = null;
        }
    }
}

