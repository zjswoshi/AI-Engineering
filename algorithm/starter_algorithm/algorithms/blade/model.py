import ast
from pathlib import Path

import onnxruntime as ort

from ...onnx_runtime import load_session

CLASSES = ("DQ", "WR", "TL", "LW", "KL", "LJ", "LJZS", "YJPS", "FYZTL", "ZGTL")


def open_model(path: Path, provider: str):
    if provider == "CUDAExecutionProvider" and hasattr(ort, "preload_dlls"):
        ort.preload_dlls(directory="")
    session = load_session(path, (provider,))
    names = ast.literal_eval(session.get_modelmeta().custom_metadata_map["names"])
    if tuple(names[index] for index in range(len(names))) != CLASSES:
        raise ValueError("Unexpected blade detector class metadata")
    if session.get_inputs()[0].shape != [1, 3, 640, 640] or session.get_outputs()[0].shape != [1, 300, 7]:
        raise ValueError("Unexpected blade detector tensor layout")
    return session
