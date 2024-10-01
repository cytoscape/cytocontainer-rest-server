
[jetty]: http://eclipse.org/jetty/
[maven]: http://maven.apache.org/
[java]: https://www.oracle.com/java/index.html
[git]: https://git-scm.com/
[rest]: https://en.wikipedia.org/wiki/Representational_state_transfer
[make]: https://www.gnu.org/software/make
[docker]: https://www.docker.com/

Cytoscape Container REST Service
===================================

[![Build Status](https://travis-ci.com/cytoscape/cytocontainer-rest-server.svg?branch=master)](https://travis-ci.com/cytoscape/cytocontainer-rest-server) 
[![Coverage Status](https://coveralls.io/repos/github/cytoscape/cytocontainer-rest-server/badge.svg)](https://coveralls.io/github/cytoscape/cytocontainer-rest-server)
[![Documentation Status](https://readthedocs.org/projects/cdaps/badge/?version=latest&token=d51549910b0a9d03167cce98f0f550cbacc48ec26e849a72a75a36c1cb474847)](https://cdaps.readthedocs.io/en/latest/?badge=latest)

Provides formatted and readily callable [REST][rest] service for several popular Cytoscape Container algorithms. 

This service runs using an embedded [Jetty][jetty] server and is invoked
from the command line. 


Requirements
=============

* MacOS, Rocky Linux 8+, Ubuntu 20+, and most other Linux distributions should work
* [Java][java] 17+ **(jdk to build)**
* [Make][make] **(to build)**
* [Cytoscape Container REST Model](https://github.com/cytoscape/cytocontainer-rest-model)
* [Maven][maven] 3.6 or higher **(to build)**
* [Docker] **(to run algorithms)**

Building Cytoscape Container REST Service
=========================================

Commands build Cytoscape Container REST Service assuming machine has [Git][git] command line tools 
installed and above Java modules have been installed.

```Bash
# In lieu of git one can just download repo and unzip it
git clone https://github.com/cytoscape/cytocontainer-rest-server.git

cd cytocontainer-rest-server
mvn clean test install
```

The above command will create a jar file under **target/** named  
**cytocontainer-rest-\<VERSION\>-jar-with-dependencies.jar** that
is a command line application

Running Cytoscape Container REST Service locally
==================================================

TODO

