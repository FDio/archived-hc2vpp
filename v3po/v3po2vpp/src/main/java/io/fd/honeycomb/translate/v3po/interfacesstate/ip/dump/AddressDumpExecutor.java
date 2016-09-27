package io.fd.honeycomb.translate.v3po.interfacesstate.ip.dump;


import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.DumpExecutionFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.i.DumpCallFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.i.DumpTimeoutException;
import io.fd.honeycomb.translate.v3po.interfacesstate.ip.dump.params.AddressDumpParams;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.IpAddressDetailsReplyDump;
import org.openvpp.jvpp.core.dto.IpAddressDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;

public class AddressDumpExecutor
        implements EntityDumpExecutor<IpAddressDetailsReplyDump, AddressDumpParams>, ByteDataTranslator,
        JvppReplyConsumer {

    private FutureJVppCore vppApi;

    public AddressDumpExecutor(@Nonnull final FutureJVppCore vppApi) {
        this.vppApi = checkNotNull(vppApi, "Vpp api refference cannot be null");
    }

    @Override
    public IpAddressDetailsReplyDump executeDump(final AddressDumpParams params) throws DumpExecutionFailedException {
        checkNotNull(params, "Address dump params cannot be null");

        IpAddressDump dumpRequest = new IpAddressDump();
        dumpRequest.isIpv6 = booleanToByte(params.isIpv6());
        dumpRequest.swIfIndex = params.getInterfaceIndex();

        try {
            return getReply(vppApi.ipAddressDump(dumpRequest).toCompletableFuture());
        } catch (TimeoutException e) {
            throw DumpTimeoutException
                    .wrapTimeoutException("Dumping or addresses ended in timeout[params : ]" + params, e);
        } catch (VppBaseCallException e) {
            throw DumpCallFailedException.wrapFailedCallException("Dumping of addresses failed[params : ]" + params, e);
        }
    }
}
