Building
----------

1) Custom ODL Oxygen build:
git clone https://git.opendaylight.org/gerrit/netconf
cd netconf
git checkout -b honeycomb
git reset --hard release/oxygen
# copy-config support:
git fetch https://git.opendaylight.org/gerrit/netconf refs/changes/06/69606/1 && git cherry-pick FETCH_HEAD
# ncclient support:
git fetch https://git.opendaylight.org/gerrit/netconf refs/changes/81/71181/1 && git cherry-pick FETCH_HEAD
mvn clean install -pl netconf/netconf-util,netconf/netconf-netty-util,netconf/mdsal-netconf-connector

2) Build vpp-integration module from hc2vpp project:
cd hc2vpp
mvn clean install -pl vpp-integration/minimal-distribution

3) (optional) Build honeycomb package
./packaging/deb/xenial/debuild.sh

4) Build ncclient
git clone -b honeycomb https://github.com/marekgr/ncclient.git
cd ncclient
sudo python setup.py install


Running examples
----------

ACL:
./acl/test_acl.sh
./acl/test_acl_updates.sh

NAT:
./acl/test_nat.sh
./acl/test_nat_updates.sh

Suggestions:
Remember that HC by default persists config and restores it after restart.
You can disable this behaviour using config/honeycomb.json.
