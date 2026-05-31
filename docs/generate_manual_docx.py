#!/usr/bin/env python3
"""
generate_manual_docx.py — Generate a .docx user manual from a Markdown source file.

Usage:
    python generate_manual_docx.py [--source <markdown_path>] [--output <docx_path>]

Defaults (when run from docs/):
    --source  <workspace>/docs/Huong_dan_su_dung_BadmintonCourtManagement.md
    --output  <workspace>/docs/Huong_dan_su_dung_BadmintonCourtManagement.docx
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

from docx import Document
from docx.enum.text import WD_PARAGRAPH_ALIGNMENT
from docx.oxml.ns import qn
from docx.shared import Inches, Pt

IMAGE_PATTERN = re.compile(r"!\[(.*?)\]\((.*?)\)")


def resolve_workspace() -> Path:
    """Derive workspace root from this script's location (2 levels up from docs/)."""
    return (Path(__file__).resolve().parent.parent).resolve()


def set_font(style, name: str = "Times New Roman", size=None) -> None:
    """Set font name for both ASCII and East Asian characters."""
    style.font.name = name
    style._element.rPr.rFonts.set(qn("w:eastAsia"), name)
    if size is not None:
        style.font.size = size


def build_docx(md_path: Path, docx_path: Path) -> Path:
    """Read a Markdown file and produce a formatted .docx document."""

    if not md_path.exists():
        print(f"Error: Source markdown file not found: {md_path}")
        sys.exit(1)

    lines = md_path.read_text(encoding="utf-8").splitlines()

    doc = Document()
    styles = doc.styles

    # Configure base fonts
    for style_name in ["Normal", "Title", "Heading 1", "Heading 2", "Heading 3"]:
        set_font(styles[style_name])
    if "Caption" in styles:
        set_font(styles["Caption"])
    styles["Normal"].font.size = Pt(12)

    number_buffer: list[str] = []

    def flush_number_buffer() -> None:
        nonlocal number_buffer
        if not number_buffer:
            return
        for item in number_buffer:
            p = doc.add_paragraph(style="List Number")
            p.add_run(item)
        number_buffer = []

    for raw in lines:
        line = raw.rstrip()
        stripped = line.strip()

        # Blank line
        if not stripped:
            flush_number_buffer()
            doc.add_paragraph("")
            continue

        # Image line, e.g.  ![caption](relative/path.png)
        image_match = IMAGE_PATTERN.fullmatch(stripped)
        if image_match:
            flush_number_buffer()
            caption, rel_path = image_match.groups()
            image_path = (md_path.parent / rel_path).resolve()
            if image_path.exists():
                pic = doc.add_paragraph()
                pic.alignment = WD_PARAGRAPH_ALIGNMENT.CENTER
                pic.add_run().add_picture(str(image_path), width=Inches(6.8))

                cap = doc.add_paragraph(caption)
                cap.alignment = WD_PARAGRAPH_ALIGNMENT.CENTER
                if cap.runs:
                    cap.runs[0].italic = True
            else:
                print(f"  Warning: Image not found, skipping: {image_path}")
            continue

        # Heading 1  (Markdown ##  →  docx Heading 1)
        if line.startswith("## "):
            flush_number_buffer()
            doc.add_paragraph(line[3:].strip(), style="Heading 1")
            continue

        # Heading 2  (Markdown ###  →  docx Heading 2)
        if line.startswith("### "):
            flush_number_buffer()
            doc.add_paragraph(line[4:].strip(), style="Heading 2")
            continue

        # Title  (Markdown #  →  docx Title, centred)
        if line.startswith("# "):
            flush_number_buffer()
            p = doc.add_paragraph(style="Title")
            p.alignment = WD_PARAGRAPH_ALIGNMENT.CENTER
            p.add_run(line[2:].strip())
            continue

        # Bullet list
        if line.startswith("- "):
            flush_number_buffer()
            p = doc.add_paragraph(style="List Bullet")
            p.add_run(line[2:].strip())
            continue

        # Numbered list (detect "1. text" pattern)
        parts = stripped.split(". ", 1)
        if len(parts) == 2 and parts[0].isdigit():
            number_buffer.append(parts[1])
            continue

        # Plain paragraph (justified)
        flush_number_buffer()
        para = doc.add_paragraph(line)
        para.alignment = WD_PARAGRAPH_ALIGNMENT.JUSTIFY

    flush_number_buffer()

    # Add space after every non-title paragraph
    for p in doc.paragraphs:
        if p.style.name != "Title":
            p.paragraph_format.space_after = Pt(6)

    doc.save(docx_path)
    return docx_path


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate a .docx user manual from a Markdown source file."
    )
    workspace = resolve_workspace()
    default_md = workspace / "docs" / "Huong_dan_su_dung_BadmintonCourtManagement.md"
    default_docx = workspace / "docs" / \
        "Huong_dan_su_dung_BadmintonCourtManagement.docx"

    parser.add_argument(
        "--source",
        type=str,
        default=str(default_md),
        help=f"Path to the source Markdown file (default: {default_md})",
    )
    parser.add_argument(
        "--output",
        type=str,
        default=str(default_docx),
        help=f"Path for the generated .docx file (default: {default_docx})",
    )
    return parser.parse_args(argv)


def main() -> None:
    args = parse_args()
    md_path = Path(args.source)
    docx_path = Path(args.output)

    print(f"Source : {md_path}")
    print(f"Output : {docx_path}")

    # Ensure output directory exists
    docx_path.parent.mkdir(parents=True, exist_ok=True)

    result = build_docx(md_path, docx_path)
    print(f"Done   : {result}")


if __name__ == "__main__":
    main()
