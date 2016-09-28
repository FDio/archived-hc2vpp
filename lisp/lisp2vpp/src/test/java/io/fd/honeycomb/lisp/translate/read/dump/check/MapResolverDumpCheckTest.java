package io.fd.honeycomb.lisp.translate.read.dump.check;

import io.fd.honeycomb.vpp.test.read.DumpCheckTest;
import org.openvpp.jvpp.core.dto.LispMapResolverDetailsReplyDump;


public class MapResolverDumpCheckTest extends DumpCheckTest<MapResolverDumpCheck, LispMapResolverDetailsReplyDump> {

    @Override
    protected MapResolverDumpCheck initCheck() {
        return new MapResolverDumpCheck();
    }

    @Override
    protected LispMapResolverDetailsReplyDump initEmptyData() {
        final LispMapResolverDetailsReplyDump replyDump = new LispMapResolverDetailsReplyDump();
        replyDump.lispMapResolverDetails = null;

        return replyDump;
    }

    @Override
    protected LispMapResolverDetailsReplyDump initValidData() {
        return new LispMapResolverDetailsReplyDump();
    }
}