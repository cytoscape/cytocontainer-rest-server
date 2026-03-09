====================================================================
Add Network Cytoscape Container REST Service Demo Service App
====================================================================

This minimal Python service demonstrates how to accept a CX2 network, make a
small change, and return the updated network payload through the CyWeb
``addNetworks`` action. It is intended as a starting template for Cytoscape
Container service apps.

What it does
------------

* Reads a network in `CX2 format <https://cytoscape.org/cx/cx2/specification/cytoscape-exchange-format-specification-(version-2)/>`__
* Adds a network attribute ``createdBy = addnetworks_demo``
* Emits the modified CX2 wrapped in a single ``addNetworks`` action

Dependencies
------------

* Python 3.11+
* ndex2

Install (for local testing)
---------------------------

.. code-block:: bash

   cd example_apps/python/addnetworks_demo
   python -m venv .venv
   source .venv/bin/activate
   pip install -r requirements.txt
   pip install -e .

Usage
-----

Read CX2 from a file and write updated network JSON to stdout:

.. code-block:: bash

   cytoscape-addnetwork-demo foo.cx2

Output format
-------------

The output JSON is a single action wrapping the modified network:

.. code-block:: json

   [{
     "action": "addNetworks",
     "data": [[ ... CX2 with added network attribute ... ]]
   }]


Container build and usage
-------------------------

This app includes a simple Dockerfile under ``docker/`` so it can be run as a
CytoContainer-style service app.

Build image
~~~~~~~~~~~

From the *addnetworks_demo* directory (same level as ``pyproject.toml``) run:

.. code-block:: bash

   cd docker

   # Build the image (same as the plain docker build command above)
   make build

Above will create `cytoscape-addnetwork-demo:latest` image. For additional configuration options
invoke `make` with no arguments

Basic usage
~~~~~~~~~~~

Show help for the service app inside the container:

.. code-block:: bash

   docker run --rm cytoscape-addnetworks-demo:latest

Run update on a local CX2 file (read-only bind mount assuming file is named foo.cx2):

.. code-block:: bash

   docker run --rm \
     -v "$(pwd)":/data \
     cytoscape-addnetworks-demo:latest \
     /data/foo.cx2

The updated network JSON will be output to stdout


Integration with Cytocontainer
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Copy ``python_addnetworksdemo.json`` directory to the directory specified by 
``cytocontainer.algorithm.conf.dir`` in the configuration for your local installation of the
Cytoscape Container REST Service and restart the Cytoscape Container REST Service.
The app will be available at the endpoint ``/v1/algorithms/python_addnetworksdemo`` 

and can be invoked with a POST request containing a CX2 network in the body and the header ``Content-Type: application/json``.

The post should be JSON and look like this:

.. code-block:: json
 
  {
    "parameters": { 
                    "Updated By": "my tool"
     }, 
    "data": [[{"CXVersion": "2.0", "hasFragments": false}, {"metaData": [{"elementCount": 1, "name": "attributeDeclarations"}, {"elementCount": 1, "name": "networkAttributes"}, {"elementCount": 1, "name": "nodes"}]}, {"attributeDeclarations": [{"networkAttributes": {"name": {"d": "string"}}, "nodes": {"name": {"d": "string"}, "represents": {"d": "string"}}}]}, {"networkAttributes": [{"name": "empty network"}]}, {"nodes": [{"id": 0, "x": 10, "y": 0, "v": {"name": "node 1", "represents": "representing node1"}}]}, {"edges": []}, {"status": [{"error": "", "success": true}]}]] 
  }

The response will be a JSON object containing id of the task

.. code-block:: json

  {
    "id": "some-uuid"
  }