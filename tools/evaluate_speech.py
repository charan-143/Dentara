"""Score locally captured dictation hypotheses without uploading transcripts."""
import argparse
import hashlib
import json
import math
import re
import statistics
from pathlib import Path

def words(text):
    return re.findall(r"\w+(?:'\w+)?", text.lower())

def distance(reference, hypothesis):
    previous = list(range(len(hypothesis) + 1))
    for i, actual in enumerate(reference, 1):
        current = [i]
        for j, predicted in enumerate(hypothesis, 1):
            current.append(min(current[-1] + 1, previous[j] + 1, previous[j-1] + (actual != predicted)))
        previous = current
    return previous[-1]

def evaluate(path):
    raw = Path(path).read_bytes()
    rows = [json.loads(line) for line in raw.decode('utf-8').splitlines() if line.strip()]
    if not rows: raise ValueError('No recordings')
    ids, errors, count, latency, seconds, matches = set(), 0, 0, [], [], 0
    for row in rows:
        if not isinstance(row['id'], str) or not row['id'] or row['id'] in ids:
            raise ValueError('Recording IDs must be nonempty and unique')
        ids.add(row['id'])
        ref, hyp = words(row['reference']), words(row['hypothesis'])
        if not ref: raise ValueError('Empty reference: evaluate silence separately')
        elapsed, duration = float(row['inference_ms']), float(row['audio_seconds'])
        if not math.isfinite(elapsed) or not math.isfinite(duration) or elapsed < 0 or duration <= 0:
            raise ValueError('Invalid timing')
        errors += distance(ref, hyp); count += len(ref); matches += int(ref == hyp)
        latency.append(elapsed); seconds.append(duration)
    return {'dataset_sha256': hashlib.sha256(raw).hexdigest(), 'recordings': len(rows), 'reference_words': count, 'word_errors': errors, 'wer': errors / count, 'exact_transcript_rate': matches / len(rows), 'inference_p50_ms': statistics.median(latency), 'inference_p95_ms': sorted(latency)[math.ceil(.95 * len(latency))-1], 'real_time_factor': sum(latency) / (1000 * sum(seconds)), 'normalization': 'lowercase and word tokenization; numbers are not expanded', 'scope': 'Offline transcript comparison. No clinical correctness or chart-field accuracy claim. Report excludes raw transcripts and recording IDs.'}

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('dataset'); parser.add_argument('--output', required=True)
    args = parser.parse_args()
    result = evaluate(args.dataset)
    output = Path(args.output); output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2), encoding='utf-8')
    print(json.dumps(result, indent=2))
