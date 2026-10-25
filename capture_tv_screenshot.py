#!/usr/bin/env python3
"""
Direct Hardware Framebuffer Screenshot Tool for Android TV / Amlogic Boxes.
Captures uncompressed 32-bit BGRA raw framebuffer from /dev/graphics/fb0
and converts it directly to a lossless PNG image.

Works even on Android TV firmware where stock 'screencap' binary is missing.

Usage:
    python capture_tv_screenshot.py [--device IP:5555] [output_path.png]
"""

import os
import sys
import argparse
import subprocess
import shutil

def find_adb():
    if shutil.which("adb"):
        return "adb"
    candidates = [
        os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"),
        os.path.expandvars(r"%ANDROID_HOME%\platform-tools\adb.exe"),
        os.path.expanduser("~/Android/Sdk/platform-tools/adb")
    ]
    for c in candidates:
        if os.path.exists(c):
            return c
    return "adb"

def find_ffmpeg():
    if shutil.which("ffmpeg"):
        return "ffmpeg"
    print("[ERROR] ffmpeg was not found in PATH.")
    print("Please install ffmpeg or add it to your system PATH.")
    sys.exit(1)

def get_connected_device(adb):
    try:
        out = subprocess.check_output([adb, "devices"], text=True)
        lines = [line.strip() for line in out.splitlines() if line.strip() and not line.startswith("List of")]
        devices = [l.split()[0] for l in lines if "\tdevice" in l]
        if len(devices) == 1:
            return devices[0]
        elif len(devices) > 1:
            return devices[0]
    except Exception:
        pass
    return None

def capture_screenshot(device, out_path="screenshot.png"):
    adb = find_adb()
    ffmpeg = find_ffmpeg()
    
    if not device:
        device = os.environ.get("ADB_DEVICE") or get_connected_device(adb)
    
    if not device:
        print("[ERROR] No ADB device target specified.")
        print("Usage: python capture_tv_screenshot.py --device <IP:PORT> [output.png]")
        sys.exit(1)

    remote_raw = "/data/local/tmp/fb0.raw"
    local_raw = os.path.abspath("temp_fb0.raw")
    
    # Query resolution
    try:
        size_out = subprocess.check_output([adb, "-s", device, "shell", "wm size"], text=True)
        parts = size_out.strip().split(":")[-1].strip().split("x")
        w, h = int(parts[0]), int(parts[1])
    except Exception:
        w, h = 1280, 720
    
    bytes_to_read = w * h * 4
    print(f"[*] Capturing {w}x{h} framebuffer from {device}...")
    
    subprocess.run([adb, "-s", device, "shell", f"dd if=/dev/graphics/fb0 of={remote_raw} bs={bytes_to_read} count=1 2>/dev/null"], check=True)
    subprocess.run([adb, "-s", device, "pull", remote_raw, local_raw], check=True, stdout=subprocess.DEVNULL)
    subprocess.run([adb, "-s", device, "shell", f"rm -f {remote_raw}"], check=True)
    
    subprocess.run([
        ffmpeg, "-y",
        "-f", "rawvideo",
        "-pix_fmt", "bgra",
        "-s", f"{w}x{h}",
        "-i", local_raw,
        "-vframes", "1",
        out_path
    ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    
    if os.path.exists(local_raw):
        os.remove(local_raw)
        
    print(f"[+] Success! Saved lossless screenshot to: {out_path} ({os.path.getsize(out_path):,} bytes)")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Capture lossless framebuffer screenshot from Android TV.")
    parser.add_argument("output", nargs="?", default="public/screenshot.png", help="Output PNG path")
    parser.add_argument("--device", "-d", help="ADB target device (e.g. <TV_IP_ADDRESS>:5555)", default=None)
    args = parser.parse_args()

    capture_screenshot(args.device, args.output)
