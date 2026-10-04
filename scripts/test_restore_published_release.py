"""Guards against wrong provenance, replaced binaries and partially restored artifacts."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import json

spec = importlib.util.spec_from_file_location('restore_release', Path(__file__).with_name('restore-published-0.2.2.py'))
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)

class RestoreTests(unittest.TestCase):
    def test_wrong_run_rejected_before_reading_files(self):
        with self.assertRaises(ValueError):
            release.restore(Path('missing'), {'id': 123})

    def test_tampered_artifact_does_not_copy_any_files(self):
        run = {'id':release.SOURCE_RUN_ID, 'head_sha':release.SOURCE_SHA, 'event':'push', 'head_branch':'v0.2.2',
               'path':'.github/workflows/publish-curseforge.yml', 'status':'completed', 'conclusion':'success'}
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)/'source'
            destination = Path(directory)/'destination'
            artifacts = []
            for relative, digest in release.EXPECTED.items():
                path = source/relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(b'replaced jar')
                artifacts.append({'jar_path':relative,'sha256':digest})
            manifest = source/'build/curseforge/release-manifest.json'
            manifest.parent.mkdir(parents=True, exist_ok=True)
            manifest.write_text(json.dumps({'version':'0.2.2','artifacts':artifacts}))
            with self.assertRaises(ValueError), patch.object(release.shutil,'copyfile') as copy:
                release.restore(source, run, destination)
            copy.assert_not_called()
            self.assertFalse(destination.exists())

if __name__ == '__main__': unittest.main()
