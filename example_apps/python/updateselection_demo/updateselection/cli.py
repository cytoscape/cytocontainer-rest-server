import argparse
import sys
from typing import Optional, Sequence

from .update import run_update


def _build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "CX2 Update Selection Demo: read a CX2 network or edgelist and emit an updated selection payload."
        )
    )
    parser.add_argument(
        dest="input",
        default="-",
        help=(
            "Input CX2 file or Edgelist file"
        ),
    )
    parser.add_argument(
        "--selectedges",
        action="store_true",
        dest="selectedges",
        default=False,
        help=(
            "Select edges if set."
        )
    )
    parser.add_argument(
        "--selectnodes",
        action="store_true",
        dest="selectnodes",
        default=False,
        help=(
            "Select nodes if set."
        ),
    )
    parser.add_argument(
        "-o",
        "--output",
        dest="output",
        default="-",
        help=(
            "Output JSON file for updated selection. Use '-' (default) to write to stdout."
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
        run_update(
            input_source=args.input,
            output_stream=out_stream,
            selectedges=args.selectedges,
            selectnodes=args.selectnodes
        )
    finally:
        if args.output != "-":
            out_stream.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
