import argparse
import sys
from typing import Optional, Sequence

from .addnewnetworks import run_addnetworks


def _build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "CX2 Add Networks Demo: read a CX2 network and emit new networks."
        )
    )
    parser.add_argument(
        dest="input",
        default="-",
        help=(
            "Input CX2 file."
        ),
    )
    parser.add_argument(
        "-o",
        "--output",
        dest="output",
        default="-",
        help=(
            "Output JSON file for new networks. Use '-' (default) to write to stdout."
        ),
    )
    parser.add_argument(
        "--createdby", default="addNetworksDemo",
        dest="createdby",
        help=(
            "Value to use for the 'createdBy' network attribute. If not provided, defaults to 'addNetworksDemo'."
        ),
    )

    return parser


def main(argv: Optional[Sequence[str]] = None) -> int:
    """
    Console script entry point.

    This is wired up in setup.py as:

    """
    if argv is None:
        argv = sys.argv[1:]

    parser = _build_arg_parser()
    args = parser.parse_args(argv)

    # Determine output stream
    if args.output == "-":
        out_stream = sys.stdout
    else:
        out_stream = open(args.output, "w", encoding="utf-8")

    try:
        run_addnetworks(
            input_source=args.input,
            output_stream=out_stream,
            createdby=args.createdby
        )
    finally:
        if args.output != "-":
            out_stream.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
