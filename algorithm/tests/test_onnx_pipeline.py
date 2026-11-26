import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

import onnx
from onnx import TensorProto, helper

from starter_algorithm.algorithms.onnx_example.pipeline import Predictor


class OnnxCpuTest(unittest.TestCase):
    def test_negation_runs_through_the_predictor(self):
        features = helper.make_tensor_value_info("features", TensorProto.FLOAT, [1, 2])
        scores = helper.make_tensor_value_info("scores", TensorProto.FLOAT, [1, 2])
        graph = helper.make_graph([helper.make_node("Neg", ["features"], ["scores"])],
                                  "negation", [features], [scores])
        model = helper.make_model(graph, opset_imports=[helper.make_opsetid("", 17)], ir_version=8)
        with TemporaryDirectory() as directory:
            path = Path(directory) / "negate.onnx"
            onnx.save(model, path)
            predictor = Predictor(path, providers=["CPUExecutionProvider"])
            self.assertEqual(predictor.session.get_providers(), ["CPUExecutionProvider"])
            self.assertEqual(predictor.predict([2, -0.5]), [-2.0, 0.5])
