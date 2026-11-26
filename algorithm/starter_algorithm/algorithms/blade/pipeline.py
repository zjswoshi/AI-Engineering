from dataclasses import dataclass
from itertools import product
from math import cos, isfinite, sin
from pathlib import Path

import numpy as np
from PIL import Image

from .model import CLASSES, open_model

INPUT_EDGE = 640
TILE_EDGE = 960
TILE_STEP = 768


@dataclass(frozen=True)
class Detection:
    label: str
    confidence: float
    polygon: list[tuple[float, float]]
    tile: tuple[int, int]

    def bounds(self):
        x, y = zip(*self.polygon)
        return min(x), min(y), max(x), max(y)


def offsets(length: int) -> list[int]:
    last = max(0, length - TILE_EDGE)
    return [*range(0, last, TILE_STEP), last]


def decode(rows, scale: float, origin: tuple[int, int], size: tuple[int, int], threshold: float):
    x, y = origin
    for cx, cy, width, height, score, category, angle in rows:
        if not all(map(isfinite, (cx, cy, width, height, score, category, angle))):
            continue
        if not (threshold <= score <= 1 and width > 0 and height > 0 and category == int(category)
                and 0 <= category < len(CLASSES) and 0 <= cx * scale < size[0] and 0 <= cy * scale < size[1]):
            continue
        polygon = []
        for dx, dy in ((-width / 2, -height / 2), (width / 2, -height / 2),
                       (width / 2, height / 2), (-width / 2, height / 2)):
            px = x + (cx + dx * cos(angle) - dy * sin(angle)) * scale
            py = y + (cy + dx * sin(angle) + dy * cos(angle)) * scale
            polygon.append((round(float(px), 2), round(float(py), 2)))
        yield Detection(CLASSES[int(category)], round(float(score), 5), polygon, origin)


def overlap(a, b) -> float:
    ax, ay, ar, ab = a
    bx, by, br, bb = b
    inter = max(0, min(ar, br) - max(ax, bx)) * max(0, min(ab, bb) - max(ay, by))
    union = (ar - ax) * (ab - ay) + (br - bx) * (bb - by) - inter
    return inter / union if union > 0 else 0.0


class Predictor:
    def __init__(self, model: Path, provider: str, confidence: float = 0.25):
        if not 0.25 <= confidence <= 1:
            raise ValueError("This model supports confidence thresholds from 0.25 to 1")
        self.session = open_model(model, provider)
        self.input_name = self.session.get_inputs()[0].name
        self.confidence = confidence

    def predict(self, image: Image.Image) -> list[Detection]:
        matches = []
        for x, y in product(offsets(image.width), offsets(image.height)):
            tile = image.crop((x, y, min(image.width, x + TILE_EDGE), min(image.height, y + TILE_EDGE)))
            edge = max(tile.size)
            square = Image.new("RGB", (edge, edge))
            square.paste(tile)
            resized = square.resize((INPUT_EDGE, INPUT_EDGE), Image.Resampling.BILINEAR)
            pixels = np.asarray(resized, dtype=np.float32).transpose(2, 0, 1)[None] / np.float32(255)
            rows = self.session.run(None, {self.input_name: np.ascontiguousarray(pixels)})[0][0]
            matches.extend(decode(rows, edge / INPUT_EDGE, (x, y), tile.size, self.confidence))

        accepted = []
        for candidate in sorted(matches, key=lambda item: item.confidence, reverse=True):
            bounds = candidate.bounds()
            if any(candidate.label == saved.label and candidate.tile != saved.tile and overlap(bounds, saved_box) >= 0.5
                   for saved, saved_box in accepted):
                continue
            accepted.append((candidate, bounds))
        return [item for item, _ in accepted]
