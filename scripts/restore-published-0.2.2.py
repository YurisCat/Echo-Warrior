#!/usr/bin/env python3
"""Restore exactly the six artifacts accepted by CurseForge's 0.2.2 release run."""
import hashlib
import json
from pathlib import Path
import shutil
import sys

SOURCE_RUN_ID = 35513312300
SOURCE_SHA = 'e460232ebbefa9ff46008a8820658f2ead286676'
EXPECTED = {
    'fabric/build/libs/echo-warrior-fabric-26.1.2-0.2.2.jar': 'ca7df24b408419efffa4261290f749ea367bc8561b3ce4b61e8c887a21f0819c',
    'neoforge/build/libs/echo-warrior-neoforge-26.1.2-0.2.2.jar': '8efecff89cbbef2a760ea3e49acede24260bceb07bd0421f687414097a61e147',
    'versions/1.21.1/fabric/build/libs/echo-warrior-fabric-1.21.1-0.2.2.jar': '7c7b7431d8520a53c1354735c61c3ef501695a129cc4c42587dd85e22e7504e6',
    'versions/1.21.1/neoforge/build/libs/echo-warrior-neoforge-1.21.1-0.2.2.jar': 'bc4bbca6133364276b2cd3b9235b75d42cd16a2ac5757d38ac2d064476e079f8',
    'versions/1.20.1/fabric/build/libs/echo-warrior-fabric-1.20.1-0.2.2.jar': '7530d87e98505bae9661cdcf976d9fb3dbc54c5f94f6278c9ffefbb4e61eefc5',
    'versions/1.20.1/forge/build/libs/echo-warrior-forge-1.20.1-0.2.2.jar': '7867147422796f06590defa293e2675833b2428ec170a691ee5ff20628f61d55',
}

def restore(source, run, destination=Path('.')):
    if not (run['id'] == SOURCE_RUN_ID and run['head_sha'] == SOURCE_SHA
            and run['event'] == 'push' and run['head_branch'] == 'v0.2.2'
            and run['path'] == '.github/workflows/publish-curseforge.yml'
            and run['status'] == 'completed' and run['conclusion'] == 'success'):
        raise ValueError('Source is not the successful, authorized CurseForge 0.2.2 release run.')
    manifest = json.loads((source/'build/curseforge/release-manifest.json').read_text(encoding='utf-8'))
    artifacts = manifest['artifacts']
    if manifest['version'] != '0.2.2' or len(artifacts) != 6 or {a['jar_path'] for a in artifacts} != set(EXPECTED):
        raise ValueError('Source manifest does not describe the six published 0.2.2 JARs.')
    for artifact in artifacts:
        relative = artifact['jar_path']
        if artifact['sha256'] != EXPECTED[relative] or hashlib.sha256((source/relative).read_bytes()).hexdigest() != EXPECTED[relative]:
            raise ValueError(f'{relative}: original release SHA-256 mismatch.')
    # Validate every file before copying any; subsequent release preparation audits contents/locales.
    for relative in EXPECTED:
        target = destination/relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source/relative, target)
    print('Restored all six original CurseForge 0.2.2 JARs with their published SHA-256 hashes.')

if __name__ == '__main__':
    restore(Path(sys.argv[1]), json.loads(Path(sys.argv[2]).read_text(encoding='utf-8')))
