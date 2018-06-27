Building
----------

1) Custom ODL Oxygen build:
git clone https://git.opendaylight.org/gerrit/netconf
cd netconf
git checkout -b honeycomb release/oxygen

# copy-config support:
git fetch https://git.opendaylight.org/gerrit/netconf refs/changes/06/69606/1 && git cherry-pick FETCH_HEAD

# ncclient support:
git fetch https://git.opendaylight.org/gerrit/netconf refs/changes/81/71181/1 && git cherry-pick FETCH_HEAD

mvn clean install -pl netconf/netconf-util,netconf/netconf-netty-util,netconf/mdsal-netconf-connector

2) Custom HC2VPP build

git clone https://gerrit.fd.io/r/hc2vpp
cd hc2vpp

a) checkout desired branch

e.g. git checkout -b stable1804 origin/stable/1804

or use master branch.

b) build vpp-integration module from hc2vpp project:

mvn clean install -pl vpp-integration/minimal-distribution

c) (optional) build honeycomb package
./packaging/deb/xenial/debuild.sh

4) Build ncclient
git clone https://github.com/ncclient/ncclient.git
cd ncclient
sudo python setup.py install


Running examples
----------
Start vpp.
Start honeycomb
(either from hc2vpp builddir or using package built in previous steps).

Then invoke:

ACL:
./acl/test_acl.sh
./acl/test_acl_updates.sh

NAT:
./acl/test_nat.sh
./acl/test_nat_updates.sh

Suggestions:
Remember that HC by default persists config and restores it after restart.
You can disable this behaviour using config/honeycomb.json.
