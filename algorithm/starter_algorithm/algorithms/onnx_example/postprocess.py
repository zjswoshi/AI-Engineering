import numpy as np


def postprocess(scores: np.ndarray) -> list[float]:
    """Return the scores for the single input item."""
    return scores[0].tolist()
