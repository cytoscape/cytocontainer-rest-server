CX2 Layout Demo
===============

This is a minimal Cytoscape Container service app implemented in Python
that does the following:

* Reads a network in `CX2 format <https://cytoscape.org/cx/cx2/specification/cytoscape-exchange-format-specification-(version-2)/>`__
* Constructs a NetworkX graph from the `CX2 <https://cytoscape.org/cx/cx2/specification/cytoscape-exchange-format-specification-(version-2)/>`__
* Computes a simple spring layout using NetworkX
* Outputs node coordinates as JSON in `updateLayouts` format `json format <https://github.com/cytoscape/cytoscape-web/wiki/updateLayouts-Example>`__

Dependencies
------------

* Python 3.11+
* ndex2
* networkx

Install (for local testing)
---------------------------

.. code-block:: bash

   cd example_apps/cx2_layout_demo
   python -m venv .venv
   source .venv/bin/activate
   pip install -r requirements.txt
   pip install -e .

Usage
-----

Read CX2 from a file and write layout JSON to stdout:

.. code-block:: bash

   cx2-layout-demo test.cx2


Output format
-------------

The output JSON looks like:

.. code-block:: json

   [{"action": "updateLayouts",
     "data": [
       { "id": 1, "x": 0.123, "y": -0.456 },
       { "id": 2, "x": -0.321, "y": 0.789 }
     ]
   }]

Only node positions are returned; all other aspects of the network are ignored.


Container build and usage
-------------------------

This app includes a simple Dockerfile under ``docker/`` so it can be run as a
CytoContainer-style service app.

Build image
~~~~~~~~~~~

From the *cx2_layout_demo* directory (same level as ``pyproject.toml``) run:

.. code-block:: bash

   cd example_apps/cx2_layout_demo

   # Build the image, tagging it as cx2-layout-demo:latest
   docker build -f docker/Dockerfile -t cx2-layout-demo:latest .

Basic usage
~~~~~~~~~~~

Show help for the service app inside the container:

.. code-block:: bash

   docker run --rm cx2-layout-demo:latest

Run layout on a local CX2 file (read-only bind mount assuming file is named test.cx2):

.. code-block:: bash

   docker run --rm \
     -v "$(pwd)":/data \
     cx2-layout-demo:latest \
     /data/test.cx2

The layout JSON will be output to stdout


Integration with Cytocontainer
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

TODO

Makefile helpers
~~~~~~~~~~~~~~~~

A simple ``Makefile`` is provided under ``docker/``. From that directory:

.. code-block:: bash

   cd example_apps/cx2_layout_demo/docker

   # Build the image (same as the plain docker build command above)
   make build

   # Run the image (prints cx2-layout-demo help by default)
   make run

   # Open an interactive shell in the container
   make shell

To push to a registry, set ``REGISTRY``:

.. code-block:: bash

   make push REGISTRY=myregistry.io/myorg

