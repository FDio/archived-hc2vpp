package io.fd.honeycomb.translate.v3po.interfacesstate.ip.dump.check;

import io.fd.honeycomb.translate.util.read.cache.EntityDumpNonEmptyCheck;
import io.fd.honeycomb.translate.util.read.cache.exceptions.check.DumpCheckFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.check.i.DumpEmptyException;
import org.openvpp.jvpp.core.dto.IpAddressDetailsReplyDump;

public class AddressDumpCheck implements EntityDumpNonEmptyCheck<IpAddressDetailsReplyDump> {

    @Override
    public void assertNotEmpty(final IpAddressDetailsReplyDump data) throws DumpCheckFailedException {
        if (data == null || data.ipAddressDetails == null) {
            throw new DumpEmptyException("Invalid data dumped");
        }
    }
}
