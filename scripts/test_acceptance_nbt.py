import gzip
from pathlib import Path
import struct
import tempfile
import unittest

from acceptance_nbt import read_nbt


def text(value):
    value=value.encode("utf-8")
    return struct.pack(">H",len(value))+value


class ReaderTests(unittest.TestCase):
    def parse(self,data):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/"fixture.dat"
            path.write_bytes(gzip.compress(data))
            return read_nbt(path)

    def test_nested_compound_inventory_and_uuid_array(self):
        data=b"\x0a"+text("")+b"\x09"+text("Inventory")+b"\x0a"+struct.pack(">i",1)
        data+=b"\x08"+text("id")+text("echo_warrior:test_echo_summoner")
        data+=b"\x0b"+text("UUID")+struct.pack(">i4i",4,-1,2,-3,4)+b"\x00\x00"
        self.assertEqual(self.parse(data),{"Inventory":[{"id":"echo_warrior:test_echo_summoner","UUID":[-1,2,-3,4]}]})

    def test_signed_long_and_float(self):
        data=b"\x0a"+text("")+b"\x04"+text("center")+struct.pack(">q",-274877906945)
        data+=b"\x05"+text("Health")+struct.pack(">f",30.5)+b"\x00"
        self.assertEqual(self.parse(data),{"center":-274877906945,"Health":30.5})

    def test_reject_corrupt_truncated_duplicate_and_trailing(self):
        valid=b"\x0a"+text("")+b"\x01"+text("a")+b"\x01\x00"
        duplicate=valid[:-1]+b"\x01"+text("a")+b"\x02\x00"
        for raw in (valid[:-2],valid+b"x",duplicate,b"\x01"+text("")+b"\x00"):
            with self.subTest(raw=raw),self.assertRaises(ValueError):
                self.parse(raw)


if __name__=="__main__":
    unittest.main()
