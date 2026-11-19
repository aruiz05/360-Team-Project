#!/usr/bin/env python3
"""Build a TP3 code/documentation review package without inventing team evidence.

Inputs must already contain generated PDFs, Javadoc and executed test evidence. The
source archive includes both production and test code plus Eclipse metadata and build
instructions. Credentials, databases, git history and build outputs are never included.
"""
from pathlib import Path
import argparse
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PDFS = ('Staff User Stories', 'Architecture', 'Detailed Design', 'Manual Tests', 'Integration Report')


def archive(folder, destination, prefix=''):
    """Store regular files in stable order with an explicit optional top-level folder."""
    with zipfile.ZipFile(destination, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as target:
        for source in sorted(folder.rglob('*')):
            if source.is_file():
                target.write(source, str(Path(prefix) / source.relative_to(folder)))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for argument in ('pdfs', 'javadoc', 'evidence', 'output'):
        parser.add_argument('--' + argument, required=True, type=Path)
    args = parser.parse_args()
    # Validate inputs before creating any package; partial evidence is never labelled complete.
    for title in PDFS:
        if not (args.pdfs / (title + '.pdf')).is_file():
            parser.error('Missing PDF: ' + title)
    for name in ('New Code Javadoc', 'Test Code Javadoc'):
        if not (args.javadoc / name / 'index.html').is_file():
            parser.error('Missing Javadoc: ' + name)
    for name in ('junit.log', 'modular-launch.log', 'gui.log', 'discovery-gui.log', 'statistics-gui.log', 'ai-gui.log'):
        if not (args.evidence / 'logs' / name).is_file():
            parser.error('Missing executed evidence: ' + name)
    if not (args.evidence / 'junit').is_dir():
        parser.error('Missing JUnit XML report folder')
    args.output.mkdir(parents=True, exist_ok=False)
    package = args.output / 'TP3'
    package.mkdir()
    for title in PDFS:
        shutil.copy2(args.pdfs / (title + '.pdf'), package)
    for name in ('New Code Javadoc', 'Test Code Javadoc'):
        shutil.copytree(args.javadoc / name, package / name)
    evidence = package / 'Test Evidence'
    shutil.copytree(args.evidence / 'logs', evidence / 'logs')
    shutil.copytree(args.evidence / 'junit', evidence / 'junit')
    commands = args.evidence / 'Verification Commands.txt'
    if commands.is_file():
        shutil.copy2(commands, evidence)
    with zipfile.ZipFile(package / 'TP3 Source.zip', 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as target:
        for source in sorted(ROOT.rglob('*')):
            relative = source.relative_to(ROOT)
            if not source.is_file():
                continue
            # An allowlist prevents accidental inclusion of local settings, keys or databases.
            if relative.parts[0] in ('src', 'test', 'scripts', 'docs', '.settings') or (
                    len(relative.parts) == 1 and (source.suffix == '.md' or source.name in ('.project', '.classpath', '.gitignore'))):
                if any(part in ('__pycache__', '.git') for part in relative.parts) or source.suffix in ('.class', '.pyc'):
                    continue
                target.write(source, str(relative))
    planning = package / 'TP3 Planning and Tracking'
    (planning / 'Status Summary Recordings').mkdir(parents=True)
    (planning / 'README.md').write_text('''# Team evidence still required

The earlier proposed planning DOCX contains unconfirmed assignments and blank dates.
Add the actual agreed Initial TP3 Backlog, Plan, and Schedule.pdf, current board evidence,
and genuine meeting summaries here. Preserve numbered plan updates starting at 2 with
change bars, or supply a truthful No Updates.pdf only if the actual plan never changed.
See Integration Report.pdf for exact timing, recording and submission requirements.
''')
    (planning / 'Status Summary Recordings' / 'README.md').write_text(
        'No meeting recording is supplied or claimed. Add the genuine Status Summary 2026-MM-DD.mp4 or .mov files.\n')
    (package / 'READINESS.md').write_text('''# Review candidate — not a complete team submission

Included: four required code/design PDFs, an additional Integration Report, both Javadoc
sets, full production/test source, reproducible scripts and actual automated evidence.

Automated result: 951 inherited checks, 91 JUnit scenarios, 133 JavaFX checks and 3
named-module launch checks passed, with zero failures. Read logs for individual evidence.
AI provider behavior in these results uses a synthetic loopback HTTP fixture.

Still required before full acceptance:
- Execute live AI verification with a valid private key; review examples and citations.
- Execute native Eclipse manual tests and save actual outcomes/screenshots.
- Add approved TP3 Team Norms.pdf and genuine planning/board/meeting evidence.
- Produce the five final team screencasts with the required durations and participation.
- Resolve the discussion-domain and video-size specification ambiguities with the instructor.
- Verify final archive/video limits, individual peer-evaluation acknowledgements and
  successful Canvas acceptance before the deadline, with each member checking.

No credential, team agreement, recording or submission acceptance has been fabricated.
''')
    destination = args.output / 'TP3-review-package.zip'
    archive(package, destination, 'TP3')
    print('Review package:', destination, '| bytes:', destination.stat().st_size)


if __name__ == '__main__':
    main()
