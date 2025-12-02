import json
import sys
from typing import Any, Dict, Iterable, Union

import networkx as nx
from ndex2.cx2 import RawCX2NetworkFactory, CX2NetworkXFactory


def _load_cx2_from_json(data: Union[Dict[str, Any], Iterable[Dict[str, Any]]]) -> nx.Graph:
    """
    Given a CX2 JSON object (dict or list of aspects), return a NetworkX graph.

    This uses the NDEx2 CX2 helpers to:

    * create a CX2Network
    * convert that CX2Network to a NetworkX graph
    """
    factory = RawCX2NetworkFactory()
    cx2_network = factory.get_cx2network(data)

    nx_factory = CX2NetworkXFactory()
    graph = nx_factory.get_graph(cx2_network)
    return graph


def load_cx2_from_file(path: str, err_stream=sys.stderr) -> nx.Graph:
    """
    Load a CX2 network from a file path into a NetworkX graph.
    """

    # any stderr message starting with @@PROGRESS followed by number
    # will be parsed and exposed by Cytoscape Container REST service
    # in the 'progress' field where 0 means no progress and 100 means
    # complete
    err_stream.write('@@PROGRESS 10\n')

    # any stderr message starting with @@MESSAGE  will be parsed by
    # Cytoscape Container REST service and exposed in 'message'
    # field of result
    err_stream.write('@@MESSAGE Loading network\n')
    err_stream.flush()
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    return _load_cx2_from_json(data)


def compute_layout(
    graph: nx.Graph,
    seed: int = 1234,
    iterations: int = 50,
    err_stream=sys.stderr
) -> Dict[Any, Dict[str, float]]:
    """
    Compute a 2D layout for the given NetworkX graph.

    Returns:
        dict mapping node_id -> {'x': float, 'y': float}
    """
    if graph.number_of_nodes() == 0:
        return {}

    # any stderr message starting with @@PROGRESS followed by number
    # will be parsed and exposed by Cytoscape Container REST service
    # in the 'progress' field where 0 means no progress and 100 means
    # complete
    err_stream.write('@@PROGRESS 30\n')

    # any stderr message starting with @@MESSAGE  will be parsed by
    # Cytoscape Container REST service and exposed in 'message'
    # field of result
    err_stream.write('@@MESSAGE Performing spring layout with ' + str(iterations) +
                     ' using ' + str(seed) + ' as seed\n')
    # spring_layout returns a dict: node -> (x, y)
    pos = nx.spring_layout(graph, seed=seed, iterations=iterations)

    layout = {}
    for node, coords in pos.items():
        x, y = float(coords[0]), float(coords[1])
        layout[node] = {"x": x, "y": y}
    return layout


def layout_to_json(layout: Dict[Any, Dict[str, float]],
                   err_stream=sys.stderr) -> Dict[str, Any]:
    """
    Convert the internal layout mapping into the JSON structure we want to emit.

    Output format:
        [{ "action": "updateLayouts",
          "data": [
            {"id": <node_id>, "x": <float>, "y": <float>},
            ...
          ]
        }]
    """
    # any stderr message starting with @@PROGRESS followed by number
    # will be parsed and exposed by Cytoscape Container REST service
    # in the 'progress' field where 0 means no progress and 100 means
    # complete
    err_stream.write('@@PROGRESS 80\n')

    # any stderr message starting with @@MESSAGE  will be parsed by
    # Cytoscape Container REST service and exposed in 'message'
    # field of result
    err_stream.write('@@MESSAGE Formating result\n')
    err_stream.flush()
    nodes = []
    for node_id, coords in layout.items():
        nodes.append(
            {
                "id": node_id,
                "x": coords["x"]*500.0,
                "y": coords["y"]*500.0,
            }
        )
    # return data structure matching specification found here:
    # https://github.com/cytoscape/cytoscape-web/wiki/updateLayouts-Example
    return [{
               "action": "updateLayouts",
               "data": nodes
             }]


def run_layout(
    input_source: str,
    output_stream,
    seed: int = 1234,
    iterations: int = 50,
) -> None:
    """
    High-level helper used by the CLI.

    Args:
        input_source: path to CX2 file, or '-' for stdin
        output_stream: file-like object to write JSON to
        seed: random seed for layout
        iterations: layout iterations
        stdin: file-like for reading when input_source == '-'
    """
    graph = load_cx2_from_file(input_source)

    layout = compute_layout(graph, seed=seed, iterations=iterations)
    result = layout_to_json(layout)

    # CytoContainer REST service expects result be output to standard out
    json.dump(result, output_stream, indent=2)
    output_stream.write("\n")
    output_stream.flush()

