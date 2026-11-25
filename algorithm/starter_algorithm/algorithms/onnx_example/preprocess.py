from collections.abc import Sequence

import numpy as np
from numpy.typing import NDArray


def preprocess(features: Sequence[float]) -> NDArray[np.float32]:
    """Convert two numeric features to a single float32 input batch."""
    tensor = np.asarray(features, dtype=np.float32)
    if tensor.shape != (2,):
        raise ValueError(f"Expected two features, got shape {tensor.shape}")
    return tensor[np.newaxis, :]
