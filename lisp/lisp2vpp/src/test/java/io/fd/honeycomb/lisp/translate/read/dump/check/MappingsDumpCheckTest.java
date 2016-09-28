package io.fd.honeycomb.lisp.translate.read.dump.check;

import io.fd.honeycomb.vpp.test.read.DumpCheckTest;
import org.openvpp.jvpp.core.dto.LispEidTableDetailsReplyDump;


public class MappingsDumpCheckTest extends DumpCheckTest<MappingsDumpCheck, LispEidTableDetailsReplyDump> {

    @Override
    protected MappingsDumpCheck initCheck() {
        return new MappingsDumpCheck();
    }

    @Override
    protected LispEidTableDetailsReplyDump initEmptyData() {
        final LispEidTableDetailsReplyDump replyDump = new LispEidTableDetailsReplyDump();
        replyDump.lispEidTableDetails = null;

        return replyDump;
    }

    @Override
    protected LispEidTableDetailsReplyDump initValidData() {
        return new LispEidTableDetailsReplyDump();
    }
}