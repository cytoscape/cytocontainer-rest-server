====================================================================
UpdateNetwork Cytoscape Container REST Service Demo Service App
====================================================================

This minimal Python service demonstrates how to accept a CX2 network, make a
small change, and return the updated network payload through the CyWeb
``updateNetwork`` action. It is intended as a starting template for Cytoscape
Container service apps.

What it does
------------

* Reads a network in `CX2 format <https://cytoscape.org/cx/cx2/specification/cytoscape-exchange-format-specification-(version-2)/>`__
* Adds a network attribute ``updatedBy = updatenetwork_demo``
* Emits the modified CX2 wrapped in a single ``updateNetwork`` action

Dependencies
------------

* Python 3.11+
* ndex2

Install (for local testing)
---------------------------

.. code-block:: bash

   cd example_apps/python/updatelayouts_demo
   python -m venv .venv
   source .venv/bin/activate
   pip install -r requirements.txt
   pip install -e .

Usage
-----

Read CX2 from a file and write updated network JSON to stdout:

.. code-block:: bash

   cytoscape-updatenetwork-demo foo.cx2

Output format
-------------

The output JSON is a single action wrapping the modified network:

.. code-block:: json

   [{
     "action": "updateNetwork",
     "data": [ ... CX2 with added network attribute ... ]
   }]


Container build and usage
-------------------------

This app includes a simple Dockerfile under ``docker/`` so it can be run as a
CytoContainer-style service app.

Build image
~~~~~~~~~~~

From the *updatelayouts_demo* directory (same level as ``pyproject.toml``) run:

.. code-block:: bash

   cd docker

   # Build the image (same as the plain docker build command above)
   make build

Above will create `cytoscape-updatenetwork-demo:latest` image. For additional configuration options
invoke `make` with no arguments

Basic usage
~~~~~~~~~~~

Show help for the service app inside the container:

.. code-block:: bash

   docker run --rm cytoscape-updatenetwork-demo:latest

Run update on a local CX2 file (read-only bind mount assuming file is named foo.cx2):

.. code-block:: bash

   docker run --rm \
     -v "$(pwd)":/data \
     cytoscape-updatenetwork-demo:latest \
     /data/foo.cx2

The updated network JSON will be output to stdout


Integration with Cytocontainer
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

TODO
