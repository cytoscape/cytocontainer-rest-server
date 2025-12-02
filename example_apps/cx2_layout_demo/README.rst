CX2 Layout Demo
===============

This is a minimal Cytoscape Container service app implemented in Python.

It:

* Reads a network in CX2 format
* Constructs a NetworkX graph from the CX2
* Computes a simple spring layout
* Outputs node coordinates as JSON

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

   cx2-layout-demo --input test.cx2

Explicit input/output files:

.. code-block:: bash

   cx2-layout-demo --input test.cx2 --output layout.json

You can also pipe CX2 on stdin:

.. code-block:: bash

   cat test.cx2 | cx2-layout-demo --input - --output -

Output format
-------------

The output JSON looks like:

.. code-block:: json

   {
     "nodes": [
       { "id": 1, "x": 0.123, "y": -0.456 },
       { "id": 2, "x": -0.321, "y": 0.789 }
     ]
   }

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

Run layout on a local CX2 file (read-only bind mount):

.. code-block:: bash

   docker run --rm \
     -v "$(pwd)":/data \
     cx2-layout-demo:latest \
     --input /data/test.cx2 --output /data/layout.json

After this, ``layout.json`` will be created (or overwritten) in the current
directory on the host.

Run with CX2 on stdin and layout JSON to stdout:

.. code-block:: bash

   cat test.cx2 | docker run --rm -i cx2-layout-demo:latest \
     --input - --output -

Integration with Cytocontainer
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When wired into a Cytocontainer REST server, the container is typically invoked
with ``cx2-layout-demo`` as the entrypoint and arguments specifying where the
input CX2 is located and where the output JSON should be written. The Dockerfile
is structured so that the container's default entrypoint is already the
``cx2-layout-demo`` console script.

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

