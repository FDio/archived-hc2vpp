package io.fd.honeycomb.lisp.translate.read.dump.check;

import io.fd.honeycomb.vpp.test.read.DumpCheckTest;
import org.openvpp.jvpp.core.dto.LispLocatorDetailsReplyDump;

public class LocatorDumpCheckTest extends DumpCheckTest<LocatorDumpCheck, LispLocatorDetailsReplyDump> {

    @Override
    protected LocatorDumpCheck initCheck() {
        return new LocatorDumpCheck();
    }

    @Override
    protected LispLocatorDetailsReplyDump initEmptyData() {
        final LispLocatorDetailsReplyDump replyDump = new LispLocatorDetailsReplyDump();
        replyDump.lispLocatorDetails = null;

        return replyDump;
    }

    @Override
    protected LispLocatorDetailsReplyDump initValidData() {
        return new LispLocatorDetailsReplyDump();
    }
}