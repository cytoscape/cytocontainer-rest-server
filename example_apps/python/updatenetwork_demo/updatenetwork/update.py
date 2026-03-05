import json
import sys
from typing import Any, Dict, Iterable, Union


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
    return data


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
    data: Union[Dict[str, Any], Iterable[Dict[str, Any]]],
    err_stream=sys.stderr
) -> Union[Dict[str, Any], Iterable[Dict[str, Any]]]:
    """
    Add a simple audit attribute to the network and return the modified CX2 data.
    """
    err_stream.write('@@PROGRESS 30\n')
    err_stream.write('@@MESSAGE Adding updatedBy attribute\n')

    if isinstance(data, list):
        meta_holder = next((entry for entry in data if "metaData" in entry), None)
        network_attr_holder = _ensure_network_attributes_aspect(data)
        network_attrs = network_attr_holder["networkAttributes"]
        network_attrs.append({
            "name": "updatedBy",
            "value": "updatenetwork_demo",
            "type": "string"
        })
        # keep metaData elementCount in sync if present
        if meta_holder and isinstance(meta_holder.get("metaData"), list):
            md_item = _find_meta_data(meta_holder["metaData"], "networkAttributes")
            if md_item is not None:
                md_item["elementCount"] = len(network_attrs)
    elif isinstance(data, dict):
        network_attrs = data.setdefault("networkAttributes", [])
        network_attrs.append({
            "name": "updatedBy",
            "value": "updatenetwork_demo",
            "type": "string"
        })
    else:
        # fall back: just return data unchanged if structure unexpected
        return data

    return data


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
    data = load_cx2_from_file(input_source)
    updated = update_network(data)
    result = network_to_json(updated)

    json.dump(result, output_stream, indent=2)
    output_stream.write("\n")
    output_stream.flush()
