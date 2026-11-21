"""Score clinician-reviewed, de-identified clinical AI cases locally."""
import argparse
import hashlib
import json
import math
import statistics
from pathlib import Path


def ratio(numerator, denominator):
    return numerator / denominator if denominator else None


def evaluate(path):
    raw = Path(path).read_bytes()
    rows = [json.loads(line) for line in raw.decode("utf-8").splitlines() if line.strip()]
    if not rows:
        raise ValueError("No evaluation cases")
    ids, latency, ram, battery = set(), [], [], []
    metrics = {
        "differential_diagnosis": {"cases": 0, "top1_agreement": 0, "expert_differential_recall": 0, "expert_differential_total": 0},
        "radiology": {"tp": 0, "fp": 0, "fn": 0, "tn": 0},
        "medication_safety": {"tp": 0, "fp": 0, "fn": 0, "tn": 0},
        "voice_charting": {"entity_tp": 0, "entity_fp": 0, "entity_fn": 0},
        "rag": {"relevant_retrieved": 0, "relevant_total": 0, "citation_correct": 0, "citation_total": 0},
        "hallucinations": {"unsupported_claims": 0, "claims": 0},
    }
    for row in rows:
        case_id = row.get("id")
        if not isinstance(case_id, str) or not case_id or case_id in ids:
            raise ValueError("Case IDs must be nonempty and unique")
        ids.add(case_id)
        if not isinstance(row.get("category"), list) or not row["category"]:
            raise ValueError(f"Case {case_id}: category must be a nonempty list")
        for category in row["category"]:
            if category not in ("differential_diagnosis", "radiology", "medication_safety", "voice_charting", "rag", "hallucinations", "performance"):
                raise ValueError(f"Case {case_id}: unknown category {category}")
        if "differential_diagnosis" in row["category"]:
            reference, predicted = set(row["expert_differentials"]), set(row["predicted_differentials"])
            if not reference: raise ValueError(f"Case {case_id}: expert_differentials must not be empty")
            m = metrics["differential_diagnosis"]; m["cases"] += 1
            m["top1_agreement"] += int(bool(row["predicted_differentials"]) and row["predicted_differentials"][0] == row["expert_differentials"][0])
            m["expert_differential_recall"] += len(reference & predicted); m["expert_differential_total"] += len(reference)
        for category, key in (("radiology", "radiology"), ("medication_safety", "medication_safety")):
            if category in row["category"]:
                pred, truth = row.get(key + "_predicted"), row.get(key + "_expert")
                if not isinstance(pred, bool) or not isinstance(truth, bool): raise ValueError(f"Case {case_id}: expected boolean {key}_predicted and {key}_expert")
                metrics[key][("tp" if pred else "fn") if truth else ("fp" if pred else "tn")] += 1
        if "voice_charting" in row["category"]:
            pred, truth = set(row.get("entities_predicted", [])), set(row.get("entities_expert", []))
            m = metrics["voice_charting"]; m["entity_tp"] += len(pred & truth); m["entity_fp"] += len(pred - truth); m["entity_fn"] += len(truth - pred)
        if "rag" in row["category"]:
            pred, truth = set(row.get("retrieved_relevant", [])), set(row.get("expert_relevant", []))
            citations, supported = row.get("citations", []), set(row.get("supported_citations", []))
            m = metrics["rag"]; m["relevant_retrieved"] += len(pred & truth); m["relevant_total"] += len(truth)
            m["citation_correct"] += len(set(citations) & supported); m["citation_total"] += len(citations)
        if "hallucinations" in row["category"]:
            claims = row.get("claims", [])
            if any(not isinstance(claim, dict) or not isinstance(claim.get("supported"), bool) for claim in claims): raise ValueError(f"Case {case_id}: claims need a boolean supported field")
            metrics["hallucinations"]["claims"] += len(claims)
            metrics["hallucinations"]["unsupported_claims"] += sum(not claim["supported"] for claim in claims)
        if "performance" in row["category"]:
            for field, values, minimum in (("latency_ms", latency, 0), ("ram_mb", ram, 0), ("battery_mwh", battery, 0)):
                value = row.get(field)
                if value is not None:
                    value = float(value)
                    if not math.isfinite(value) or value < minimum: raise ValueError(f"Case {case_id}: invalid {field}")
                    values.append(value)
    d = metrics["differential_diagnosis"]
    d["expert_differential_recall_at_k"] = ratio(d.pop("expert_differential_recall"), d.pop("expert_differential_total"))
    d["top1_expert_agreement"] = ratio(d.pop("top1_agreement"), d["cases"])
    for key in ("radiology", "medication_safety"):
        m = metrics[key]; m["sensitivity"] = ratio(m["tp"], m["tp"] + m["fn"]); m["specificity"] = ratio(m["tn"], m["tn"] + m["fp"])
        m["alert_precision"] = ratio(m["tp"], m["tp"] + m["fp"]); m["alert_recall"] = m["sensitivity"]
    v = metrics["voice_charting"]; v["entity_extraction_f1"] = ratio(2 * v["entity_tp"], 2 * v["entity_tp"] + v["entity_fp"] + v["entity_fn"])
    r = metrics["rag"]; r["recall_at_k"] = ratio(r["relevant_retrieved"], r["relevant_total"]); r["citation_accuracy"] = ratio(r["citation_correct"], r["citation_total"])
    h = metrics["hallucinations"]; h["unsupported_claim_rate"] = ratio(h["unsupported_claims"], h["claims"])
    performance = {f"{name}_{label}": statistics.quantiles(values, n=100, method="inclusive")[percentile - 1] if len(values) > 1 else (values[0] if values else None) for name, values in (("latency_ms", latency), ("ram_mb", ram), ("battery_mwh", battery)) for label, percentile in (("p50", 50), ("p95", 95))}
    return {"dataset_sha256": hashlib.sha256(raw).hexdigest(), "cases": len(rows), "metrics": metrics, "performance": performance,
            "scope": "Offline metrics on clinician-reviewed cases. Does not establish clinical safety or efficacy. Synthetic data is provided; replace only with properly de-identified, approved data.",
            "privacy": "Report excludes case IDs and clinical text."}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("dataset"); parser.add_argument("--output", required=True)
    args = parser.parse_args()
    result = evaluate(args.dataset)
    output = Path(args.output); output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(json.dumps(result, indent=2))
