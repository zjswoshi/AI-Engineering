from collections.abc import Sequence
from pathlib import Path

import onnxruntime as ort

DEFAULT_PROVIDERS = ("CPUExecutionProvider",)


def load_session(
    model_path: Path,
    providers: Sequence[str] = DEFAULT_PROVIDERS,
) -> ort.InferenceSession:
    """Load a model and verify that the requested execution providers are active."""
    if not model_path.is_file():
        raise FileNotFoundError(f"ONNX model not found: {model_path}")
    if not providers:
        raise ValueError("Select at least one ONNX execution provider")
    available = ort.get_available_providers()
    if unavailable := [provider for provider in providers if provider not in available]:
        raise RuntimeError(f"Unavailable ONNX providers: {unavailable}. Available: {available}")
    session = ort.InferenceSession(str(model_path), providers=list(providers))
    active = session.get_providers()
    if inactive := [provider for provider in providers if provider not in active]:
        raise RuntimeError(f"Inactive ONNX providers: {inactive}. Active: {active}")
    session.disable_fallback()
    return session
