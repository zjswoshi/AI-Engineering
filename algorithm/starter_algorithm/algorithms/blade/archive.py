import io
import json
import shutil
import warnings
from collections import Counter
from pathlib import Path
from uuid import UUID
from zipfile import BadZipFile, ZipFile

from PIL import Image, ImageOps, UnidentifiedImageError

from .model import CLASSES
from .report import preview, write_pdf

SUPPORTED = {".jpg", ".jpeg", ".png"}
MAX_MEMBER_BYTES = 100_000_000
MAX_PIXELS = 50_000_000


class InvalidArchive(ValueError):
    pass


def inspect_archive(task: dict, data_dir: Path, predictor, font_path: str, expanded_limit: int) -> None:
    archive = data_dir / "uploads" / f"{UUID(str(task['taskId']))}.zip"
    attempt = str(UUID(str(task["attemptId"])))
    reports = data_dir / "reports"
    reports.mkdir(parents=True, exist_ok=True)
    output = reports / attempt
    if (output / "report.pdf").is_file() and (output / "summary.json").is_file():
        return
    staging = reports / f".{attempt}.partial"
    staging.mkdir()
    try:
        summary, samples = process_archive(task, archive, staging, predictor, expanded_limit)
        write_pdf(staging / "report.pdf", summary, samples, font_path)
        write_json(staging / "summary.json", summary)
        staging.replace(output)
    finally:
        if staging.exists():
            shutil.rmtree(staging)


def process_archive(task, archive, output, predictor, expanded_limit):
    tally = Counter(dict.fromkeys(CLASSES, 0))
    samples = []
    image_count = 0
    hit_count = 0
    try:
        with ZipFile(archive, metadata_encoding="gb18030") as package:
            entries = package.infolist()
            if len(entries) > 100_000 or sum(item.file_size for item in entries) > expanded_limit:
                raise InvalidArchive("archive limits exceeded")
            for info in entries:
                if info.is_dir() or info.filename.startswith("__MACOSX/"):
                    continue
                if Path(info.filename).suffix.lower() not in SUPPORTED:
                    continue
                image = read_image(package, info)
                try:
                    detections = predictor.predict(image)
                    tally.update(item.label for item in detections)
                    hit_count += bool(detections)
                    image_path = output / f"image-{image_count}.jpg"
                    preview(image, detections, image_path)
                    if detections and len(samples) < 6:
                        samples.append((info.filename, image_path))
                    write_json(output / f"image-{image_count}.json", {
                        "index": image_count, "name": info.filename, "width": image.width, "height": image.height,
                        "detections": [{"label": item.label, "confidence": item.confidence, "polygon": item.polygon}
                                       for item in detections],
                    })
                    image_count += 1
                finally:
                    image.close()
    except (BadZipFile, UnicodeError) as failure:
        raise InvalidArchive("ZIP directory or image CRC invalid") from failure
    if not image_count:
        raise InvalidArchive("no supported images")
    return ({**{key: task[key] for key in ("taskId", "farmName", "turbineNumber", "originalName", "createdAt")},
             "model": "blade_defect_v1.1.onnx", "imageCount": image_count, "defectImageCount": hit_count,
             "detectionCount": tally.total(), "classes": dict(tally)}, samples)


def read_image(package, info):
    if not 0 < info.file_size <= MAX_MEMBER_BYTES:
        raise InvalidArchive("image byte limit exceeded")
    try:
        with package.open(info) as member:
            data = member.read(MAX_MEMBER_BYTES + 1)
        if len(data) != info.file_size:
            raise InvalidArchive("invalid image length")
        with warnings.catch_warnings():
            warnings.simplefilter("error", Image.DecompressionBombWarning)
            with Image.open(io.BytesIO(data), formats=["JPEG", "PNG"]) as source:
                if source.width * source.height > MAX_PIXELS:
                    raise InvalidArchive("image pixel limit exceeded")
                return ImageOps.exif_transpose(source).convert("RGB")
    except (UnidentifiedImageError, Image.DecompressionBombError, Image.DecompressionBombWarning,
            OSError, EOFError, RuntimeError, NotImplementedError, ValueError) as failure:
        raise InvalidArchive("invalid image data") from failure


def write_json(output: Path, content):
    output.write_text(json.dumps(content, ensure_ascii=False, allow_nan=False), encoding="utf-8")
