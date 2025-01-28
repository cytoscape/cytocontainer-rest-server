The instructions in this readme provide steps
to install Cytoscape Container REST service as a service managed by systemd.
These instructions use the files in this directory and require
a Rocky Linux 8 box with superuser access.

# Requirements

* podman (dnf install podman)
* cytorunner user added (useradd cytorunner)
* cytorunner user added to docker group (usermod -a -G docker cytorunner)
* Java 17+ installed and in path
* if doing apache run this: /usr/sbin/setsebool -P httpd_can_network_connect=1
  to enable the port redirection

1) Create needed directories as super user

mkdir -p /var/log/cytocontainer
chown cytorunner.cytorunner /var/log/cytocontainer
mkdir -p /opt/cytocontainer/tasks
ln -s /var/log/cytocontainer /opt/cytocontainer/logs
chown -R cytorunner.cytorunner /opt/cytocontainer

2) Create conf file

Copy server.conf to /etc/cytocontainer/


3) Create algorithms file

Copy algorithms under algorithms/ directory to /etc/cytocontainer/algorithms


4) Copy jar

Build Cytoscape Container REST service jar with dependencies and put
in /opt/cytocontainer directory. Create a symlink to the specific jar named cytocontainer-rest.jar
(symlink won't work for systemd)

5) Create systemd file

Copy cytocontainer.service to /etc/systemd/system
cd /etc/systemd/system

6) Register script with systemd

systemctl daemon-reload
cd /etc/systemd/system
systemctl enable cytocontainer
systemctl start cytocontainer

7) Verify its running

ps -elf | grep cytocontainer

# output
4 S cytorunner 18929     1 19  80   0 - 9280026 futex_ 11:57 ?      00:00:01 /bin/java -jar /opt/cytocontainer/cytocontainer-rest.jar --mode runserver --conf /etc/cytocontainer.conf
