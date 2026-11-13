"""Short file sharing conflicts must not stop a server or replay commands."""
import importlib.util
import json
from pathlib import Path
import unittest
from unittest.mock import Mock, patch

spec = importlib.util.spec_from_file_location("acceptance_server", Path(__file__).with_name("pet-acceptance-server.py"))
server = importlib.util.module_from_spec(spec)
spec.loader.exec_module(server)


class ControlReads(unittest.TestCase):
    def test_short_sharing_conflict_then_one_payload(self):
        path = Mock()
        path.read_text.side_effect = [PermissionError("shared"), PermissionError("shared"),
                                      '{"commands":["save-all flush"]}']
        with patch.object(server.time, "sleep") as wait:
            self.assertEqual(server.read_control_json(path), {"commands": ["save-all flush"]})
        self.assertEqual(path.read_text.call_count, 3)
        self.assertEqual(wait.call_count, 2)

    def test_permanent_access_failure_is_bounded(self):
        path = Mock()
        path.read_text.side_effect = PermissionError("denied")
        with patch.object(server.time, "sleep") as wait, self.assertRaises(PermissionError):
            server.read_control_json(path, attempts=4)
        self.assertEqual(path.read_text.call_count, 4)
        self.assertEqual(wait.call_count, 3)

    def test_invalid_json_is_not_retried(self):
        path = Mock()
        path.read_text.return_value = '{"commands":'
        with patch.object(server.time, "sleep") as wait, self.assertRaises(json.JSONDecodeError):
            server.read_control_json(path)
        path.read_text.assert_called_once()
        wait.assert_not_called()

    def test_other_io_failure_is_not_hidden(self):
        path = Mock()
        path.read_text.side_effect = OSError("device failure")
        with patch.object(server.time, "sleep") as wait, self.assertRaises(OSError):
            server.read_control_json(path)
        path.read_text.assert_called_once()
        wait.assert_not_called()


if __name__ == "__main__":
    unittest.main()
