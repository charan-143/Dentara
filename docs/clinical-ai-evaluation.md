# Clinical AI evaluation

Run the local, offline evaluator against synthetic or approved de-identified, expert-reviewed JSONL cases:

```powershell
python tools/evaluate_clinical_ai.py tools/clinical_ai_eval_cases.example.jsonl --output .local/clinical-ai-report.json
python -m unittest tools/tests/test_evaluate_clinical_ai.py
```

Each row has a unique `id`, one or more `category` values, and category-specific fields. The example file documents the schema. Differential cases compare ranked predictions with expert-ranked differentials. Radiology and medication safety use boolean expert and predicted labels to calculate sensitivity, specificity, and alert precision/recall. Voice cases compare extracted entity sets for F1. RAG cases compare retrieved relevant passage IDs (Recall@K) and supported citations. Hallucination cases count expert-labeled supported and unsupported claims. Performance rows may provide latency in milliseconds, peak RAM in MB, and battery consumption in mWh; p50 and p95 are reported where measurements exist. Capture device metrics on representative hardware and workloads; this offline runner does not measure them itself.

Empty denominators are reported as `null`. Reports contain an input SHA-256 digest and aggregate metrics only, not case IDs or clinical text. Keep real patient data out of the sample dataset and follow local approval and de-identification requirements for any real evaluation corpus. These offline measurements do not establish clinical safety or efficacy.

AI-generated clinical findings must remain pending until a clinician explicitly reviews and applies them. Voice charting automatic apply is disabled; the existing copilot chart action is an explicit clinician-triggered save.
