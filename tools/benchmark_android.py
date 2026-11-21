"""Measure process-cold startup on an explicitly selected connected device.

No app data is cleared. The target app is force-stopped before each launch.
No report is uploaded and no UI content is captured.
"""
import argparse
import json
import math
import platform
import re
import statistics
import subprocess
from datetime import datetime, timezone
from pathlib import Path

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--adb', default='adb')
    p.add_argument('--serial', required=True)
    p.add_argument('--package', required=True)
    p.add_argument('--runs', type=int, default=10)
    p.add_argument('--output', required=True)
    args = p.parse_args()
    if args.runs < 3: p.error('Use at least three runs')
    if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_.]+', args.package): p.error('Invalid package')
    def adb(*command):
        result = subprocess.run([args.adb, '-s', args.serial, *command], check=True, capture_output=True, text=True, timeout=60)
        return result.stdout.strip()
    if adb('get-state') != 'device': raise RuntimeError('Device not ready')
    resolved = adb('shell', 'cmd', 'package', 'resolve-activity', '--brief', args.package)
    component = resolved.splitlines()[-1]
    if not re.fullmatch(r'[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+', component):
        raise RuntimeError('Could not resolve launcher activity: ' + resolved)
    durations = []
    for _ in range(args.runs):
        adb('shell', 'am', 'force-stop', args.package)
        result = adb('shell', 'am', 'start', '-W', '-n', component)
        if not re.search(r'^Status:\s*ok\s*$', result, re.MULTILINE):
            raise RuntimeError('Launch failed: ' + result)
        match = re.search(r'^TotalTime:\s*(\d+)\s*$', result, re.MULTILINE)
        if not match: raise RuntimeError('No startup timing: ' + result)
        durations.append(int(match.group(1)))
    report = {'timestamp_utc': datetime.now(timezone.utc).isoformat(), 'package': args.package, 'component': component, 'device_model': adb('shell','getprop','ro.product.model'), 'android_release': adb('shell','getprop','ro.build.version.release'), 'build_fingerprint': adb('shell','getprop','ro.build.fingerprint'), 'runs': args.runs, 'startup_total_ms': durations, 'p50_ms': statistics.median(durations), 'p95_ms': sorted(durations)[math.ceil(.95 * len(durations))-1], 'host': platform.platform(), 'scope': 'am start -W TotalTime after force-stop. Process-cold, not disk-cache-cold; not time-to-interactive. Device must be unlocked. App data is retained. Record app revision/build type and thermal conditions separately.'}
    output = Path(args.output); output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report, indent=2))

if __name__ == '__main__': main()
