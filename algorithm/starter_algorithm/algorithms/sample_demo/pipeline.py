from hashlib import sha256


def detect(sample: str) -> dict[str, str | float]:
    """Return a repeatable demonstration result for an opaque sample ID."""
    score = 0.5 + sha256(sample.encode("utf-8")).digest()[0] / 510
    return {"label": "demo", "score": round(score, 3)}
