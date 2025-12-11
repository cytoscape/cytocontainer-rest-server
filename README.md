[jetty]: http://eclipse.org/jetty/
[maven]: http://maven.apache.org/
[java]: https://www.oracle.com/java/index.html
[git]: https://git-scm.com/
[rest]: https://en.wikipedia.org/wiki/Representational_state_transfer
[make]: https://www.gnu.org/software/make
[docker]: https://www.docker.com/

# Cytoscape Container REST Service

The **Cytoscape Container REST Service** is a reference implementation of a **Service App host** for **Cytoscape Web**. It provides a REST interface that executes Docker-packaged analysis tools (Service Apps) in isolated containers, captures their output, and returns results to Cytoscape Web according to the Service App Framework specification.

This server powers the official Cytoscape Web Service App endpoint:

**https://cd.ndexbio.org/cy/**

The Service App Framework allows Cytoscape Web to call external analytical services, enabling Cytoscape Web to remain lightweight while integrating a wide variety of computational tools.

---

## Table of Contents

- [Background](#background)
- [How the Container REST Service Works](#how-the-container-rest-service-works)
- [Architecture](#architecture)
- [Service App Specification](#service-app-specification)
- [Buiding and Running the Server](#building-and-running-the-server)
- [Development Setup](#development-setup)
- [License](#license)

---

## Background

Cytoscape Web supports extensibility through **Service Apps**—external REST services that take user-provided data, run an analysis, and return structured results.

Useful references:

- [Service App Framework overview](https://cytoscape-web.readthedocs.io/en/latest/Extending.html#service-apps)
- [Service App API specification](<https://github.com/cytoscape/cytoscape-web/wiki/Specification-for-Service-App-in-Cytoscape-Web-(draft-v2)>)

This repository provides a ready-to-use server implementation that:

- Implements the _Service App_ REST contract
- Manages execution of Service Apps inside Docker containers
- Handles input file distribution and output streaming

Tools written in **any language** can become Service Apps as long as they follow the input/output conventions.

---

## How the Container REST Service Works

Each Service App is packaged as a Docker image. When Cytoscape Web sends a request:

1. The server receives an uploaded input file.
2. The server launches the corresponding Service App Docker container.
3. The container:
   - Reads a single input file mounted inside the container.
   - Performs the requested analysis.
   - Writes its response to **STDOUT**.
4. The server captures STDOUT and streams it back to Cytoscape Web.

This design enforces strong isolation, reproducibility, and language independence.

---

## Architecture

```
Cytoscape Web
       |
       | REST Request (with input file)
       v
Container REST Service  -------------------------
|  Validate request                              |
|  Write input file                              |
|  Launch Docker container (Service App image)   |
|  Capture stdout                                |
|  Return response                               |
--------------------------------------------------
       |
       v
   Cytoscape Web (displays results)
```

Key architectural goals:

- **Stateless for requests** – each execution is independent.
- **Docker sandboxing** – no direct execution of host binaries.
- **Minimal API surface** – just enough to satisfy the Service App spec.
- **Log transparency** – container logs are captured and returned if needed. (Return endpoints to be implemented)

---

## Service App Specification

Every Service App Docker container must follow these rules:

1. **Execution**  
   The container _entrypoint_ executes its own command line tool that may contain a single input file path along with optional command line flags

2. **Output**  
   The container writes the final service output to **STDOUT** adhering to JSON structure defined
   in the Service App Framework specification.

3. **Real time task progress**

   Lines of format `@@MESSAGE <TEXT>\n` output to **STDERR** by the container will be captured and used to set the `message` value in the REST endpoint. Lines of format `@@PROGRESS #\n` where `#` should be a value 0-100 output to **STDERR** by the container will be captured and used to set the `progress` value in the REST endpoint.

3. **No network dependency required**  
   Containers should not depend on external network calls unless explicitly needed.

Specification reference:  
https://github.com/cytoscape/cytoscape-web/wiki/Specification-for-Service-App-in-Cytoscape-Web-(draft-v2)

---

## Building and running the Server

### Requirements

- MacOS, Rocky Linux 8+, Ubuntu 20+, and most other Linux distributions should work
- [Java][java] 17+ **(jdk to build)**
- [Make][make] **(to build)**
- [Cytoscape Container REST Model](https://github.com/cytoscape/cytocontainer-rest-model)
- [Maven][maven] 3.6 or higher **(to build)**
- [Docker] **(to run algorithms)**

### Building

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

---

## Development Setup

Clone the repo:

```bash
git clone https://github.com/cytoscape/cytocontainer-rest-server
cd cytocontainer-rest-server
```

Run tests:

```bash
make test
# or
# mvn test
```

TODO

---

## License

This project is released under the **MIT License**. See `LICENSE` for details.
