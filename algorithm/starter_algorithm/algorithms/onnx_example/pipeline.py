from collections.abc import Sequence
from pathlib import Path

from ...onnx_runtime import DEFAULT_PROVIDERS
from .model import INPUT_NAME, MODEL_PATH, OUTPUT_NAME, load_model
from .postprocess import postprocess
from .preprocess import preprocess


class Predictor:
    """Load one ONNX session and reuse it across calls to predict."""

    def __init__(
        self,
        model_path: Path = MODEL_PATH,
        providers: Sequence[str] = DEFAULT_PROVIDERS,
    ) -> None:
        self.session = load_model(model_path, providers)

    def predict(self, features: Sequence[float]) -> list[float]:
        tensor = preprocess(features)
        (scores,) = self.session.run([OUTPUT_NAME], {INPUT_NAME: tensor})
        return postprocess(scores)
