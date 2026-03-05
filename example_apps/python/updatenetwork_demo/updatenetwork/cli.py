import argparse
import sys
from typing import Optional, Sequence

from .update import run_update


def _build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "CX2 Update Network Demo: read a CX2 network and emit an updated network payload."
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
            "Output JSON file for updated network. Use '-' (default) to write to stdout."
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
        )
    finally:
        if args.output != "-":
            out_stream.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
