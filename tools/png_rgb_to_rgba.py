"""Rewrite an 8-bit RGB PNG as RGBA (opaque alpha). Standard library only."""
import struct, sys, zlib

def chunks(data):
    pos = 8
    while pos < len(data):
        n, = struct.unpack(">I", data[pos:pos + 4]); kind = data[pos + 4:pos + 8]
        yield kind, data[pos + 8:pos + 8 + n]; pos += 12 + n

def paeth(a, b, c):
    p = a + b - c; pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    return a if pa <= pb and pa <= pc else b if pb <= pc else c

src, dst = sys.argv[1], sys.argv[2]
data = open(src, "rb").read()
idat = b""; ihdr = None
for kind, body in chunks(data):
    if kind == b"IHDR": ihdr = body
    elif kind == b"IDAT": idat += body
w, h, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", ihdr)
assert depth == 8 and ctype == 2 and interlace == 0, "expects 8-bit RGB, non-interlaced"
raw = zlib.decompress(idat); bpp = 3; stride = w * bpp
prev = bytearray(stride); out = bytearray(); i = 0
for _ in range(h):
    f = raw[i]; line = bytearray(raw[i + 1:i + 1 + stride]); i += 1 + stride
    for x in range(stride):
        a = line[x - bpp] if x >= bpp else 0; b = prev[x]; c = prev[x - bpp] if x >= bpp else 0
        if f == 1: line[x] = (line[x] + a) & 255
        elif f == 2: line[x] = (line[x] + b) & 255
        elif f == 3: line[x] = (line[x] + (a + b) // 2) & 255
        elif f == 4: line[x] = (line[x] + paeth(a, b, c)) & 255
    out.append(0)
    for x in range(0, stride, 3): out += line[x:x + 3] + b"\xff"
    prev = line

def chunk(kind, body):
    return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body))

png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)) \
    + chunk(b"IDAT", zlib.compress(bytes(out), 9)) + chunk(b"IEND", b"")
open(dst, "wb").write(png)
