"""Export the manually edited draw.io source; never regenerate the layout.

Requires Python 3.9+, pypdf, and draw.io Desktop.
Example (Windows):
  python reexport-er.py --drawio "C:/Users/NAME/AppData/Local/Programs/draw.io/draw.io.exe"
"""
import argparse
import base64
import os
from pathlib import Path
import subprocess
import tempfile
import xml.etree.ElementTree as ET
import urllib.parse
import zlib

from pypdf import PdfReader, PdfWriter, Transformation


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--drawio', required=True, type=Path, help='Official draw.io Desktop executable')
    args = parser.parse_args()
    folder = Path(__file__).resolve().parent
    source = folder / 'RMS_ER_Models.drawio'
    model = ET.parse(source).getroot()
    pages = model.findall('diagram')
    if len(pages) != 2:
        raise ValueError('Expected two model pages: core, complete')
    for index, expected in enumerate(((96, 19), (151, 34))):
        root = pages[index].find('./mxGraphModel/root')
        if root is None:
            # The desktop app may compress its XML when the user saves it.
            text = urllib.parse.unquote(zlib.decompress(base64.b64decode(pages[index].text), -15).decode('utf-8'))
            root = ET.fromstring(text).find('root')
        counts = tuple(len(root.findall(f"object[@rmsKind='{kind}']")) for kind in ('entity', 'field', 'foreignKey'))
        if counts != (13, *expected):
            raise ValueError(f'Model counts changed on page {index+1}: {counts}')
    startupinfo = None
    if os.name == 'nt':
        startupinfo = subprocess.STARTUPINFO()
        startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW
        startupinfo.wShowWindow = subprocess.SW_HIDE

    def export(target, options):
        command = [str(args.drawio), '--export', '--theme', 'light', '--disable-update', '--timeout', '45', *options, '--output', str(target), str(source)]
        subprocess.run(command, check=True, startupinfo=startupinfo, timeout=90)
        if not target.is_file() or target.stat().st_size == 0:
            raise RuntimeError(f'Export missing: {target.name}')

    with tempfile.TemporaryDirectory(prefix='rms-er-export-') as temp:
        temp = Path(temp)
        raw = temp / 'native-pages.pdf'
        export(raw, ['--all-pages', '--crop'])
        reader = PdfReader(raw)
        if len(reader.pages) != 2:
            raise ValueError('Expected exactly two exported pages')
        for index, stem in enumerate(('core-er', 'complete-er')):
            page = reader.pages[index]
            w, h = float(page.mediabox.width), float(page.mediabox.height)
            tw, th = 1190.551181, 841.889764  # A3 landscape, points
            scale = min((tw-20)/w, (th-20)/h)
            writer = PdfWriter()
            target = writer.add_blank_page(tw, th)
            target.merge_transformed_page(page, Transformation().scale(scale).translate((tw-w*scale)/2, (th-h*scale)/2))
            writer.add_metadata({'/Title': 'RMS '+stem, '/Creator': 'draw.io Desktop; vector A3 export'})
            writer.write(temp / (stem+'.pdf'))
            export(temp / (stem+'.svg'), ['--page-index', str(index+1), '--size', 'page', '--embed-diagram', '--embed-svg-metadata', '--embed-svg-fonts', 'false'])
            export(temp / (stem+'-preview.png'), ['--page-index', str(index+1), '--size', 'page', '--width', '2400'])
        # Replace repository outputs only after all exports succeed.
        for stem in ('core-er', 'complete-er'):
            for suffix in ('.pdf', '.svg', '-preview.png'):
                (folder / (stem+suffix)).write_bytes((temp / (stem+suffix)).read_bytes())
    print('Updated core and complete PDF/SVG/PNG from the editable source.')


if __name__ == '__main__':
    main()
