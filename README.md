
[jetty]: http://eclipse.org/jetty/
[maven]: http://maven.apache.org/
[java]: https://www.oracle.com/java/index.html
[git]: https://git-scm.com/
[rest]: https://en.wikipedia.org/wiki/Representational_state_transfer
[make]: https://www.gnu.org/software/make
[cdapp]: https://github.com/cytoscape/cy-community-detection
[docker]: https://www.docker.com/

Community Detection REST Service
===================================

[![Build Status](https://travis-ci.com/cytoscape/communitydetection-rest-server.svg?branch=master)](https://travis-ci.com/cytoscape/communitydetection-rest-server) 
[![Coverage Status](https://coveralls.io/repos/github/cytoscape/communitydetection-rest-server/badge.svg)](https://coveralls.io/github/cytoscape/communitydetection-rest-server)
[![Documentation Status](https://readthedocs.org/projects/cdaps/badge/?version=latest&token=d51549910b0a9d03167cce98f0f550cbacc48ec26e849a72a75a36c1cb474847)](https://cdaps.readthedocs.io/en/latest/?badge=latest)

Provides formatted and readily callable [REST][rest] service for several popular Community Detection algorithms. 
The service is used by the [Cytoscape Community Detection App][cdapp]

This service runs using an embedded [Jetty][jetty] server and is invoked
from the command line. 

**If you use Community Detection REST Service in your research, please cite:**

Singhal A, Cao S, Churas C, Pratt D, Fortunato S, Zheng F, et al. (2020) Multiscale community detection in Cytoscape. PLoS Comput Biol 16(10): e1008239. https://doi.org/10.1371/journal.pcbi.1008239


Requirements
=============

* MacOS, Centos 6+, Ubuntu 12+, and most other Linux distributions should work
* [Java][java] 11+ **(jdk to build)**
* [Make][make] **(to build)**
* [Maven][maven] 3.3 or higher **(to build)** -- tested with 3.6
* [Docker] **(to run algorithms)**

Building Community Detection REST Service
=========================================

Commands build Community Detection REST Service assuming machine has [Git][git] command line tools 
installed and above Java modules have been installed.

```Bash
# In lieu of git one can just download repo and unzip it
git clone https://github.com/cytoscape/communitydetection-rest-server.git

cd communitydetection-rest-server
mvn clean test install
```

The above command will create a jar file under **target/** named  
**communitydetection-rest-\<VERSION\>-jar-with-dependencies.jar** that
is a command line application

Running Community Detection REST Service locally
==================================================

[Click here for instructions on how to run the service locally](https://github.com/cytoscape/communitydetection-rest-server/wiki/Running-Community-Detection-REST-Service-locally)

Algorithms
===========

Each algorithm has its own repository and is packaged as a
[Docker](docker) image

### Community Detection Algorithms

 * [CliXO](https://github.com/idekerlab/cdclixo)

 * [Infomap](https://github.com/idekerlab/cdinfomap)

 * [Louvain](https://github.com/idekerlab/cdlouvain)

 * [OSLOM](https://github.com/idekerlab/cdoslom)

### Functional Enrichment/Term Mapping Algorithms

 * [Enrichr](https://github.com/idekerlab/cdenrichrgenestoterm)

 * [iQuery](https://github.com/idekerlab/cdiquerygenestoterm)

 * [gProfiler](https://github.com/idekerlab/cdgprofilergenestoterm)

COPYRIGHT AND LICENSE
=====================

[Click here](LICENSE)

Acknowledgements
================

TODO
