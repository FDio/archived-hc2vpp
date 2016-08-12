# Honyecomb is a noarch package, so this isn't necessary. It's also very slow.
%define __jar_repack 0
%define _version %(./version)
%define _release %(./release)

Name:       honeycomb
Version:    %{_version}
# The Fedora/CentOS packaging guidelines *require* the use of a disttag. Honeycomb's
#   RPM build doesn't do anything Fedora/CentOS specific, so the disttag is
#   unnecessary and unused in our case, but both the docs and the pros (apevec)
#   agree that we should include it.
# See: https://fedoraproject.org/wiki/Packaging:DistTag
Release:    %{_release}
BuildArch:  noarch
Summary:    fd.io Honeycomb
Group:      Applications/Communications
License:    Apache-1.0
URL:        http://www.fd.io
Source0:     vpp-integration-distribution-%{_version}-SNAPSHOT-hc.zip
Source1:     honeycomb.service
Requires:    vpp, java >= 1:1.8.0
# Required for creating honeycomb group
Requires(pre): shadow-utils
# Required for configuring systemd
BuildRequires: systemd

%pre
# Create `honeycomb` user/group
# Short circuits if the user/group already exists
# Home dir must be a valid path for various files to be created in it
getent passwd honeycomb > /dev/null || useradd honeycomb -M -d $RPM_BUILD_ROOT/opt/%name
getent group honeycomb > /dev/null || groupadd honeycomb
getent group vpp > /dev/null && usermod -a -G vpp honeycomb

%description
fd.io Honeycomb

%prep
# Extract Source0 (Honeycomb archive)
%autosetup -n vpp-integration-distribution-%{_version}-SNAPSHOT

%install
# Create directory in build root for Honeycomb
mkdir -p $RPM_BUILD_ROOT/opt/%name
# Copy Honeycomb from archive to its dir in build root
cp -r ../vpp-integration-distribution-%{_version}-SNAPSHOT/* $RPM_BUILD_ROOT/opt/%name
# Create directory in build root for systemd .service file
mkdir -p $RPM_BUILD_ROOT/%{_unitdir}
# Copy Honeycomb's systemd .service file to correct dir in build root
echo "PWD:$PWD"
cp ${RPM_BUILD_ROOT}/../../%{name}.service $RPM_BUILD_ROOT/%{_unitdir}/%name.service

%postun
#   When the RPM is removed, the subdirs containing new files wouldn't normally
#   be deleted. Manually clean them up.
#   Warning: This does assume there's no data there that should be preserved
if [ $1 -eq 0 ]; then
    rm -rf $RPM_BUILD_ROOT/opt/%name
fi

%files
# Honeycomb will run as honeycomb:honeycomb, set as user:group for honeycomb dir, don't override mode
%attr(-,honeycomb,honeycomb) /opt/%name
# Configure systemd unitfile user/group/mode
%attr(0644,root,root) %{_unitdir}/%name.service

