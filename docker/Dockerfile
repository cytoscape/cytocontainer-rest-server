FROM rockylinux:9.3.20231119

RUN dnf update -y
RUN dnf install podman java-21-openjdk-devel -y
RUN chmod 1777 /tmp
RUN mkdir -p /etc/cytocontainer/algorithms
RUN mkdir -p /opt/cytocontainer
RUN mkdir /tmp/cytocontainer_logs
RUN mkdir /tmp/cytocontainer_tasks

COPY systemdservice/algorithms/*.json /etc/cytocontainer/algorithms/
COPY systemdservice/server.conf /etc/cytocontainer/
COPY target/cytocontainer-rest-server*jar /opt/cytocontainer/cytocontainer-rest-server.jar

EXPOSE 8081

ENTRYPOINT ["/bin/java"]
CMD ["-jar", "/opt/cytocontainer/cytocontainer-rest-server.jar", "--mode", "runserver", "--conf", "/etc/cytocontainer/server.conf"]
