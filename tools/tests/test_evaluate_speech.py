import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
spec = importlib.util.spec_from_file_location('evaluate_speech', Path(__file__).parents[1] / 'evaluate_speech.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class SpeechMetricsTests(unittest.TestCase):
    def test_edit_distance(self):
        self.assertEqual(module.distance(['tooth','fourteen'], ['tooth','forty']), 1)
        self.assertEqual(module.distance(['a'], []), 1)
        self.assertEqual(module.distance([], ['a','b']), 2)

    def test_corpus_weighting_and_privacy(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'synthetic.jsonl'
            rows = [{'id':'1','reference':'tooth fourteen depth three','hypothesis':'tooth fourteen depth two','inference_ms':100,'audio_seconds':2}, {'id':'2','reference':'bleeding','hypothesis':'','inference_ms':200,'audio_seconds':1}]
            path.write_text('\n'.join(json.dumps(r) for r in rows), encoding='utf-8')
            report = module.evaluate(path)
            self.assertEqual(report['wer'], .4)
            self.assertAlmostEqual(report['real_time_factor'], .1)
            self.assertNotIn('tooth', json.dumps(report))
            rows[0]['audio_seconds'] = 0
            path.write_text('\n'.join(json.dumps(r) for r in rows), encoding='utf-8')
            with self.assertRaises(ValueError): module.evaluate(path)
