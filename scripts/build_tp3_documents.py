#!/usr/bin/env python3
"""Render the versioned TP3 outlines and exact Graphviz models into reviewable PDFs.

Requires ReportLab, Pillow and the Graphviz dot executable. This deliberately supports
only the headings, paragraphs, bullet lists, tables and local diagrams used by these
sources; it never fetches remote content or claims manual acceptance evidence.
"""
from pathlib import Path
import argparse
import html
import re
import subprocess
import tempfile

from PIL import Image as PillowImage
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.platypus import (SimpleDocTemplate, Paragraph, Spacer, Image,
                                LongTable, TableStyle, KeepTogether, CondPageBreak)

ROOT = Path(__file__).resolve().parents[1]
TITLES = ('Staff User Stories', 'Architecture', 'Detailed Design', 'Manual Tests', 'Integration Report')
WIDTH = letter[0] - 94


def inline(text):
    """Escape XML while preserving the small inline markup used in our own documents."""
    text = html.escape(text)
    text = re.sub(r'`([^`]+)`', r'<font name="Courier">\1</font>', text)
    text = re.sub(r'\*\*([^*]+)\*\*', r'<b>\1</b>', text)
    return text


def styles():
    result = getSampleStyleSheet()
    result.add(ParagraphStyle('BodyTP3', fontName='Helvetica', fontSize=10.5, leading=13,
                             spaceAfter=5, alignment=TA_LEFT, splitLongWords=True, allowWidows=0, allowOrphans=0))
    result.add(ParagraphStyle('BulletTP3', parent=result['BodyTP3'], leftIndent=12,
                             firstLineIndent=0, bulletIndent=0, spaceAfter=4))
    result.add(ParagraphStyle('CellTP3', parent=result['BodyTP3'], fontSize=8.5, leading=11,
                             spaceAfter=0))
    for name, size in [('Title', 22), ('Heading1', 15), ('Heading2', 12)]:
        result[name].fontName = 'Helvetica-Bold'
        result[name].fontSize = size
        result[name].leading = size + 4
        result[name].textColor = colors.black
        result[name].spaceBefore = 12 if name != 'Title' else 0
        result[name].spaceAfter = 8
        result[name].keepWithNext = False
    result.add(ParagraphStyle('CaptionTP3', parent=result['BodyTP3'], fontSize=9, leading=12,
                             spaceBefore=4, spaceAfter=9))
    return result


def footer(canvas, document):
    canvas.saveState()
    canvas.setFont('Helvetica', 8)
    canvas.drawString(47, 28, 'CSE 360 | TP3 review candidate | November 19, 2026')
    canvas.drawRightString(letter[0] - 47, 28, str(document.page))
    canvas.restoreState()


def render(source, destination, temporary):
    s = styles()
    story = []
    lines = source.read_text().splitlines()
    i = 0
    pending_heading = None
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1
            continue
        if line.startswith('#'):
            level = len(line) - len(line.lstrip('#'))
            style = 'Title' if level == 1 else ('Heading1' if level == 2 else 'Heading2')
            heading = Paragraph(inline(line[level:].strip()), s[style])
            next_line = next((value.strip() for value in lines[i+1:] if value.strip()), '')
            if next_line.startswith('!['):
                pending_heading = heading
            else:
                heading.keepWithNext = not next_line.startswith('|')
                if next_line.startswith('|'):
                    story.append(CondPageBreak(80))
                story.append(heading)
            i += 1
            continue
        match = re.fullmatch(r'!\[(.*?)\]\((.*?)\)', line)
        if match:
            caption, relative = match.groups()
            dot = (source.parent / relative).with_suffix('.dot')
            png = temporary / (dot.stem + '.png')
            subprocess.run(['dot', '-Tpng', '-Gdpi=180', str(dot), '-o', str(png)], check=True)
            with PillowImage.open(png) as picture:
                width, height = picture.size
            scale = min(WIDTH / width, (350 if dot.stem == 'architecture' else 400) / height)
            image = Image(str(png), width=width * scale, height=height * scale)
            group = [image, Paragraph(inline(caption), s['CaptionTP3'])]
            if pending_heading is not None:
                group.insert(0, pending_heading)
                pending_heading = None
            story.append(KeepTogether(group))
            i += 1
            continue
        if line.startswith('|'):
            rows = []
            while i < len(lines) and lines[i].strip().startswith('|'):
                cells = [cell.strip() for cell in lines[i].strip().strip('|').split('|')]
                if not all(re.fullmatch(r':?-+:?', cell) for cell in cells):
                    rows.append(cells)
                i += 1
            columns = len(rows[0])
            ratios = {3: [.23, .40, .37], 4: [.23, .21, .30, .26]}.get(columns, [1 / columns] * columns)
            # Results tables need short numeric columns, unlike the requirement matrix.
            if rows[0][1:3] == ['Passed', 'Failed']:
                ratios = [.37, .10, .10, .43]
            table = LongTable([[Paragraph(inline(cell), s['CellTP3']) for cell in row] for row in rows],
                              colWidths=[WIDTH * value for value in ratios], repeatRows=1,
                              hAlign='LEFT', splitByRow=1)
            table.setStyle(TableStyle([
                ('VALIGN', (0, 0), (-1, -1), 'TOP'), ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#eeeeee')),
                ('BOX', (0, 0), (-1, -1), .4, colors.grey), ('INNERGRID', (0, 0), (-1, -1), .25, colors.lightgrey),
                ('LEFTPADDING', (0, 0), (-1, -1), 5), ('RIGHTPADDING', (0, 0), (-1, -1), 5),
                ('TOPPADDING', (0, 0), (-1, -1), 5), ('BOTTOMPADDING', (0, 0), (-1, -1), 5)]))
            story.extend([table, Spacer(1, 9)])
            continue
        if line.startswith('- '):
            story.append(Paragraph(inline(line[2:]), s['BulletTP3'], bulletText='\u2022'))
            i += 1
            continue
        text = [line]
        i += 1
        while i < len(lines) and lines[i].strip() and not lines[i].lstrip().startswith(('#', '- ', '|', '![')):
            text.append(lines[i].strip())
            i += 1
        story.append(Paragraph(inline(' '.join(text)), s['BodyTP3']))
    document = SimpleDocTemplate(str(destination), pagesize=letter, leftMargin=47, rightMargin=47,
                                 topMargin=43, bottomMargin=45, title=source.stem,
                                 author='CSE 360 Team Project')
    document.build(story, onFirstPage=footer, onLaterPages=footer)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    with tempfile.TemporaryDirectory(prefix='tp3-diagrams-') as temporary:
        for title in TITLES:
            render(ROOT / 'docs/tp3' / (title + '.md'), args.output / (title + '.pdf'), Path(temporary))
            print('Generated:', args.output / (title + '.pdf'))


if __name__ == '__main__':
    main()
