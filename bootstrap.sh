#!/usr/bin/env bash

# install needed packages (maven is needed to build REST service)
dnf install -y podman java-11-openjdk maven git

# create cytorunner user
adduser cytorunner

# create log directory
mkdir -p /var/log/cytocontainer
chown cytorunner.cytorunner /var/log/cytocontainer

# create tasks directory
mkdir -p /opt/cytocontainer/tasks
ln -s /var/log/cytocontainer /opt/cytocontainer/logs
chown -R cdrunner.cdrunner /opt/cytocontainer

# build REST jar
pushd /vagrant/
mvn install -Dmaven.test.skip=true
JAR_PATH=`/bin/ls target/cytocontainer-rest-*with*dependencies.jar`
JAR_FILE=`basename $JAR_PATH`
cp $JAR_PATH /opt/cytocontainer/.
popd
pushd /opt/cytocontainer
ln -s /opt/cytocontainer/${JAR_FILE} cytocontainer-rest.jar
popd

# create config directory for service
mkdir -p /etc/cytocontainer/algorithms

# copy configuration from systemd/ 
cp /vagrant/systemdservice/server.conf /etc/cytocontainer/.

# copy algorithms from systemd/ 
cp /vagrant/systemdservice/algorithms/*.json /etc/cytocontainer/algorithms/.

# copy systemd service
cp /vagrant/systemdservice/cytocontainer.service /lib/systemd/system

# enable cytocontainer service
systemctl enable cytocontainer

echo "Starting Cytoscape Container REST service"
# start service
systemctl start cytocontainer

systemctl status cytocontainer

echo ""
echo "Cytoscape Container REST Service server configured"
echo "Configuration file: /etc/cytocontainer/server.conf"
echo "Log files: /var/log/cytocontainer"
echo "Task directory: /opt/cytocontainer/tasks"
echo "To stop service: systemctl stop cytocontainer"
echo "To start service: systemctl start cytocontainer"  
echo ""
echo "Visit http://localhost:8081/cy in your browser for swagger"
echo ""
echo "To update the service:"
echo " 1) Create new jar via mvn install from VM or host computer"
echo " 2) Connect to VM via vagrant ssh and become root (sudo -u root /bin/bash)"
echo " 3) Copy /vagrant/target/cyto*jar to /opt/cytocontainer"
echo " 4) Update /opt/cytocontainer/cytocontainer-rest.jar symlink if needed"
echo " 5) Run systemctl restart cytocontainer"
echo ""
echo "Have a nice day!!!"
echo ""

