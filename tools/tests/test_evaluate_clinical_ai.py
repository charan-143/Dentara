import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location('evaluate_clinical_ai', Path(__file__).parents[1] / 'evaluate_clinical_ai.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class ClinicalAiMetricsTests(unittest.TestCase):
    def test_example_metrics_and_report_privacy(self):
        report = module.evaluate(Path(__file__).parents[1] / 'clinical_ai_eval_cases.example.jsonl')
        self.assertEqual(report['metrics']['differential_diagnosis']['top1_expert_agreement'], 1)
        self.assertEqual(report['metrics']['radiology']['sensitivity'], 1)
        self.assertEqual(report['metrics']['medication_safety']['sensitivity'], 0)
        self.assertEqual(report['metrics']['voice_charting']['entity_extraction_f1'], 1)
        self.assertEqual(report['metrics']['rag']['citation_accuracy'], .5)
        self.assertEqual(report['metrics']['hallucinations']['unsupported_claim_rate'], .5)
        self.assertNotIn('synthetic-001', json.dumps(report))

    def test_rejects_duplicate_ids(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'cases.jsonl'
            row = {'id': 'dup', 'category': ['performance']}
            path.write_text(json.dumps(row) + '\n' + json.dumps(row), encoding='utf-8')
            with self.assertRaises(ValueError): module.evaluate(path)


if __name__ == '__main__':
    unittest.main()
