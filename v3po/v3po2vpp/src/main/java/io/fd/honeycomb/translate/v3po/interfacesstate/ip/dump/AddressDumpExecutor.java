package io.fd.honeycomb.translate.v3po.interfacesstate.ip.dump;


import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.v3po.interfacesstate.ip.dump.params.AddressDumpParams;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.vpp.jvpp.core.dto.IpAddressDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.IpAddressDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AddressDumpExecutor
        implements EntityDumpExecutor<IpAddressDetailsReplyDump, AddressDumpParams>, ByteDataTranslator,
        JvppReplyConsumer {

    private FutureJVppCore vppApi;

    public AddressDumpExecutor(@Nonnull final FutureJVppCore vppApi) {
        this.vppApi = checkNotNull(vppApi, "Vpp api refference cannot be null");
    }

    @Override
    @Nonnull
    public IpAddressDetailsReplyDump executeDump(final InstanceIdentifier<?> identifier, final AddressDumpParams params)
            throws ReadFailedException {
        checkNotNull(params, "Address dump params cannot be null");

        IpAddressDump dumpRequest = new IpAddressDump();
        dumpRequest.isIpv6 = booleanToByte(params.isIpv6());
        dumpRequest.swIfIndex = params.getInterfaceIndex();

        return getReplyForRead(vppApi.ipAddressDump(dumpRequest).toCompletableFuture(), identifier);
    }
}
