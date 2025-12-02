import argparse
import sys
from typing import Optional, Sequence

from .layout import run_layout


def _build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "CX2 Layout Demo: read a CX2 network and emit node layout coordinates as JSON."
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
            "Output JSON file for layout. Use '-' (default) to write to stdout."
        ),
    )

    parser.add_argument(
        "--seed",
        type=int,
        default=1234,
        help="Random seed for the spring layout (default: 1234).",
    )

    parser.add_argument(
        "--iterations",
        type=int,
        default=50,
        help="Number of iterations for the spring layout (default: 50).",
    )

    return parser


def main(argv: Optional[Sequence[str]] = None) -> int:
    """
    Console script entry point.

    This is wired up in setup.py as:

        cx2-layout-demo = cx2layoutdemo.cli:main
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
        run_layout(
            input_source=args.input,
            output_stream=out_stream,
            seed=args.seed,
            iterations=args.iterations
        )
    finally:
        if args.output != "-":
            out_stream.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

