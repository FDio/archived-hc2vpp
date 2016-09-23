package io.fd.honeycomb.translate.v3po.interfacesstate.ip.dump.params;

public class AddressDumpParams {

    private final int interfaceIndex;
    private final boolean isIpv6;

    public AddressDumpParams(final int interfaceIndex, final boolean isIpv6) {
        this.interfaceIndex = interfaceIndex;
        this.isIpv6 = isIpv6;
    }

    public int getInterfaceIndex() {
        return interfaceIndex;
    }

    public boolean isIpv6() {
        return isIpv6;
    }

    @Override
    public String toString() {
        return "AddressDumpParams{" +
                "interfaceIndex=" + interfaceIndex +
                ", isIpv6=" + isIpv6 +
                '}';
    }
}
