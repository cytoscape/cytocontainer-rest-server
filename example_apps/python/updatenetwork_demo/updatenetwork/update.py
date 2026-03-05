import json
import sys
from typing import Any, Dict, Iterable, Union
from ndex2.cx2 import CX2Network, RawCX2NetworkFactory

def load_cx2_from_file(path: str, err_stream=sys.stderr) -> Union[Dict[str, Any], Iterable[Dict[str, Any]]]:
    """
    Load a CX2 network from a file path (or '-' for stdin) into memory.
    """
    err_stream.write('@@PROGRESS 10\n')
    err_stream.write('@@MESSAGE Loading network\n')
    err_stream.flush()

    if path == "-":
        data = json.load(sys.stdin)
    else:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
    cx2_netfactory = RawCX2NetworkFactory()
    return cx2_netfactory.get_cx2network(data)


def _find_meta_data(meta_data, name: str):
    if not isinstance(meta_data, list):
        return None
    for item in meta_data:
        if item.get("name") == name:
            return item
    return None


def _ensure_network_attributes_aspect(data) -> Dict[str, Any]:
    """
    Return (and create if necessary) the networkAttributes aspect holder.
    """
    if isinstance(data, list):
        holder = next((entry for entry in data if "networkAttributes" in entry), None)
        if holder is None:
            holder = {"networkAttributes": []}
            data.append(holder)
        return holder
    if isinstance(data, dict):
        holder = data.get("networkAttributes")
        if holder is None:
            data["networkAttributes"] = []
            holder = data["networkAttributes"]
        return {"networkAttributes": holder}
    return {"networkAttributes": []}


def update_network(
    net: CX2Network,
    err_stream=sys.stderr) -> CX2Network:
    """
    Add a simple audit attribute to the network and return the modified CX2 data.
    """
    err_stream.write('@@PROGRESS 30\n')
    err_stream.write('@@MESSAGE Adding updatedBy attribute\n')

    net.add_network_attribute(key="updatedBy",
                              value="updateNetworkDemo", datatype="string")

    return net.to_cx2()


def network_to_json(data: Union[Dict[str, Any], Iterable[Dict[str, Any]]],
                    err_stream=sys.stderr) -> Dict[str, Any]:
    """
    Wrap the CX2 payload in a CyWeb-compatible updateNetwork action.
    """
    err_stream.write('@@PROGRESS 80\n')
    err_stream.write('@@MESSAGE Formatting updated network\n')
    err_stream.flush()
    return [{
        "action": "updateNetwork",
        "data": data
    }]


def run_update(
    input_source: str,
    output_stream,
) -> None:
    """
    High-level helper used by the CLI.
    """
    net = load_cx2_from_file(input_source)
    updated = update_network(net)
    result = network_to_json(updated)

    json.dump(result, output_stream, indent=2)
    output_stream.write("\n")
    output_stream.flush()
