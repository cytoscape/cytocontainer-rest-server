import json
import sys
import random
from typing import Any, Dict, Iterable, Union
import pandas as pd
from ndex2.cx2 import CX2Network, RawCX2NetworkFactory


# create a function to detect if the input is CX2 or 
# edgelist by looking at content and then call the appropriate loader function. This is a bit hacky but it allows us to 
# support both formats without requiring the user to specify which one they are using.
def load_network_from_file(path: str, err_stream=sys.stderr) -> Union[CX2Network, pd.DataFrame]:
    """
    Load network from a file path (or '-' for stdin) into memory.
    """
    err_stream.write('@@PROGRESS 10\n')
    err_stream.write('@@MESSAGE Loading network\n')
    err_stream.flush()

    if path == "-":
        # read the first few bytes to determine if it's CX2 or edgelist
        peek = sys.stdin.read(100)
        sys.stdin.seek(0)
        if peek.lstrip().startswith("[") and "{" in peek:
            return load_cx2_from_file(path, err_stream=err_stream)
        else:      
            return pd.read_csv(sys.stdin, sep="\t")
    else:
        with open(path, 'r') as f:
            peek = f.read(100)
            f.seek(0)
            if peek.lstrip().startswith("[") and "{" in peek:
                return load_cx2_from_file(path, err_stream=err_stream)
            else:
                return pd.read_csv(f, sep="\t")

def load_cx2_from_file(path: str, err_stream=sys.stderr) -> CX2Network:
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



def load_edgelist_from_file(path: str, err_stream=sys.stderr) -> pd.DataFrame:
    """
    Load edgelist from a file path (or '-' for stdin) into memory.
    """
    err_stream.write('@@PROGRESS 10\n')
    err_stream.write('@@MESSAGE Loading network\n')
    err_stream.flush()

    if path == "-":
        df = pd.read_csv(sys.stdin, sep="\t")
    else:
        df = pd.read_csv(path, sep="\t")
    return df

def get_selected_nodes_and_edges_from_cx2(
    cx2: CX2Network,
    err_stream=sys.stderr,
    selectedges: bool = False,
    selectnodes: bool = False
) -> Dict[str, Any]:
    """
    Get a random selection of nodes and edges from the CX2 network.
    """

    err_stream.write('@@PROGRESS 30\n')
    err_stream.write('@@MESSAGE Selecting nodes and edges from network\n')
    selection = {"nodes": [], "edges": []}
    err_stream.flush()

    node_ids = [node_id for node_id,_ in cx2.get_nodes().items()]
    edge_ids = [edge_id for edge_id,_ in cx2.get_edges().items()]
    
    if selectnodes and len(node_ids) > 0:
        selected_node_ids = random.sample(node_ids, min(3, len(node_ids)))
        selection["nodes"] = selected_node_ids

    if selectedges and len(edge_ids) > 0:
        selected_edge_ids = random.sample(edge_ids, min(3, len(edge_ids)))
        selection["edges"] = selected_edge_ids

    return selection

def get_selected_nodes_and_edges_from_edgelist(
    df: pd.DataFrame,
    err_stream=sys.stderr,
    selectedges: bool = False,
    selectnodes: bool = False
) -> Dict[str, Any]:
    """
    Get a random selection of nodes and edges from the data frame.
    """

    err_stream.write('@@PROGRESS 30\n')
    err_stream.write('@@MESSAGE Selecting nodes and edges from edgelist\n')
    selection = {"nodes": [], "edges": []}
    err_stream.flush()

    # get all node ids and randomly select some of them
    node_ids = df["source_id"].tolist() + df["target_id"].tolist()
    node_ids = list(set(node_ids))
    edge_ids = df["edge_id"].tolist()
    
    if selectnodes and len(node_ids) > 0:
        selected_node_ids = random.sample(node_ids, min(3, len(node_ids)))
        selection["nodes"] = selected_node_ids

    if selectedges and len(edge_ids) > 0:
        selected_edge_ids = random.sample(edge_ids, min(3, len(edge_ids)))
        selection["edges"] = selected_edge_ids

    return selection


def selection_to_json(selection: Dict[str, Any], err_stream=sys.stderr) -> Dict[str, Any]:
    """
    Wrap thepayload in a CyWeb-compatible updateSelection action.
    """
    err_stream.write('@@PROGRESS 80\n')
    err_stream.write('@@MESSAGE Formatting updated network\n')
    err_stream.flush()
    return [{
        "action": "updateSelection",
        "data": selection
    }]


def run_update(
    input_source: str,
    output_stream=sys.stdout,
    err_stream=sys.stderr,
    selectedges: bool = False,
    selectnodes: bool = False
) -> None:
    """
    High-level helper used by the CLI.
    """

    data = load_network_from_file(input_source, err_stream=err_stream)
    if isinstance(data, CX2Network):
        updated = get_selected_nodes_and_edges_from_cx2(data, err_stream=err_stream,
                                           selectedges=selectedges,
                                           selectnodes=selectnodes)
    else:
        updated = get_selected_nodes_and_edges_from_edgelist(data, err_stream=err_stream,
                                                             selectedges=selectedges,
                                                             selectnodes=selectnodes)
    result = selection_to_json(updated, err_stream=err_stream)

    json.dump(result, output_stream, indent=2)
    output_stream.write("\n")
    output_stream.flush()
