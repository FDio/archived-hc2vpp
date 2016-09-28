package io.fd.honeycomb.lisp.translate.read.dump.check;

import io.fd.honeycomb.vpp.test.read.DumpCheckTest;
import org.openvpp.jvpp.core.dto.LispEidTableMapDetailsReplyDump;


public class VniTableDumpCheckTest extends DumpCheckTest<VniTableDumpCheck, LispEidTableMapDetailsReplyDump> {

    @Override
    protected VniTableDumpCheck initCheck() {
        return new VniTableDumpCheck();
    }

    @Override
    protected LispEidTableMapDetailsReplyDump initEmptyData() {
        final LispEidTableMapDetailsReplyDump replyDump = new LispEidTableMapDetailsReplyDump();
        replyDump.lispEidTableMapDetails = null;

        return replyDump;
    }

    @Override
    protected LispEidTableMapDetailsReplyDump initValidData() {
        return new LispEidTableMapDetailsReplyDump();
    }
}