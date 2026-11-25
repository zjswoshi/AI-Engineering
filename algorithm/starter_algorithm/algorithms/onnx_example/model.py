from collections.abc import Sequence
from pathlib import Path

from onnxruntime import InferenceSession

from ...onnx_runtime import DEFAULT_PROVIDERS, load_session

MODEL_PATH = Path(__file__).with_name("models") / "model.onnx"
INPUT_NAME = "features"
OUTPUT_NAME = "scores"


def load_model(
    model_path: Path = MODEL_PATH,
    providers: Sequence[str] = DEFAULT_PROVIDERS,
) -> InferenceSession:
    """Load the weights for this algorithm."""
    return load_session(model_path, providers)
