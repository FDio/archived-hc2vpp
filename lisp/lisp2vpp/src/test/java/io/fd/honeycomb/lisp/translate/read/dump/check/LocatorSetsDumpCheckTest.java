package io.fd.honeycomb.lisp.translate.read.dump.check;

import io.fd.honeycomb.vpp.test.read.DumpCheckTest;
import org.openvpp.jvpp.core.dto.LispLocatorSetDetailsReplyDump;


public class LocatorSetsDumpCheckTest extends DumpCheckTest<LocatorSetsDumpCheck, LispLocatorSetDetailsReplyDump> {

    @Override
    protected LocatorSetsDumpCheck initCheck() {
        return new LocatorSetsDumpCheck();
    }

    @Override
    protected LispLocatorSetDetailsReplyDump initEmptyData() {
        final LispLocatorSetDetailsReplyDump replyDump = new LispLocatorSetDetailsReplyDump();
        replyDump.lispLocatorSetDetails = null;

        return replyDump;
    }

    @Override
    protected LispLocatorSetDetailsReplyDump initValidData() {
        return new LispLocatorSetDetailsReplyDump();
    }
}