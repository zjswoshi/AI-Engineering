from dataclasses import dataclass
from math import ceil, floor
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


from .pipeline import Detection

MAX_SAMPLE_IMAGES = 6
MAX_DETAILS_PER_IMAGE = 4
OVERVIEW_SIZE = (990, 620)
DETAIL_SIZE = (520, 250)
MARK = "#ffc04a"
INK = "#173440"


@dataclass(frozen=True)
class DefectEvidence:
    detection: Detection
    path: Path


@dataclass(frozen=True)
class SampleEvidence:
    name: str
    source_size: tuple[int, int]
    count: int
    overview: Path
    details: list[DefectEvidence]


def preview(image, detections, path: Path):
    view = image.copy()
    view.thumbnail((1440, 1440), Image.Resampling.LANCZOS)
    scale_x, scale_y = view.width / image.width, view.height / image.height
    draw = ImageDraw.Draw(view)
    for item in detections:
        polygon = [(round(x * scale_x), round(y * scale_y)) for x, y in item.polygon]
        draw.line([*polygon, polygon[0]], fill="#ffc04a", width=3)
        anchor = max(0, min(x for x, _ in polygon)), max(0, min(y for _, y in polygon) - 14)
        draw.text(anchor, f"{item.label} {item.confidence:.2f}", fill="#fff6d9", stroke_width=2, stroke_fill="#302427")
    view.save(path, quality=85)


def _crop_axis(low: float, high: float, limit: int, margin: float) -> tuple[int, int]:
    start = max(0, min(limit - 1, floor(low)))
    stop = max(start + 1, min(limit, ceil(high)))
    length = min(limit, max(96, stop - start + 2 * ceil(margin)))
    start = max(0, min(limit - length, (start + stop - length) // 2))
    return start, start + length


def _outline(draw, polygon, width):
    ring = [*polygon, polygon[0]]
    draw.line(ring, fill=INK, width=width + 2)
    draw.line(ring, fill=MARK, width=width)


def _closeup(image: Image.Image, detection: Detection, path: Path):
    x1, y1, x2, y2 = detection.bounds()
    visible_width = min(x2, image.width) - max(x1, 0)
    visible_height = min(y2, image.height) - max(y1, 0)
    margin = 0.45 * max(0, visible_width, visible_height)
    left, right = _crop_axis(x1, x2, image.width, margin)
    top, bottom = _crop_axis(y1, y2, image.height, margin)
    width, height = right - left, bottom - top
    scale = min(DETAIL_SIZE[0] / width, DETAIL_SIZE[1] / height)
    size = max(1, round(width * scale)), max(1, round(height * scale))
    with image.resize(size, Image.Resampling.LANCZOS, box=(left, top, right, bottom)) as detail:
        polygon = [(round((x - left) * detail.width / width), round((y - top) * detail.height / height))
                   for x, y in detection.polygon]
        _outline(ImageDraw.Draw(detail), polygon, 2)
        detail.save(path, quality=92)


def sample_evidence(image: Image.Image, detections: list[Detection], name: str, folder: Path, index: int):
    overview = folder / f"overview-{index}.jpg"
    scale = min(1, OVERVIEW_SIZE[0] / image.width, OVERVIEW_SIZE[1] / image.height)
    size = max(1, round(image.width * scale)), max(1, round(image.height * scale))
    with image.resize(size, Image.Resampling.LANCZOS) as view:
        view.save(overview, quality=90)
    selected = sorted(detections, key=lambda item: item.confidence, reverse=True)[:MAX_DETAILS_PER_IMAGE]
    selected.sort(key=lambda item: item.bounds()[1] + item.bounds()[3])
    details = []
    for number, detection in enumerate(selected, start=1):
        path = folder / f"detail-{index}-{number}.jpg"
        _closeup(image, detection, path)
        details.append(DefectEvidence(detection, path))
    return SampleEvidence(name, image.size, len(detections), overview, details)


def _draw_overview(canvas, draw, sample: SampleEvidence, font):
    x, y = 85, 285
    width, height = OVERVIEW_SIZE
    draw.rectangle((x, y, x + 1070, y + height), fill="#edf3f4")
    with Image.open(sample.overview) as image:
        left = x + (width - image.width) // 2
        top = y + (height - image.height) // 2
        canvas.paste(image, (left, top))
        sx, sy = image.width / sample.source_size[0], image.height / sample.source_size[1]
    polygons = [[(round(left + px * sx), round(top + py * sy))
                 for px, py in detail.detection.polygon] for detail in sample.details]
    # Put badges in their own column so adjacent defects cannot hide a number.
    label_x = x + width + 38
    previous_y = y - 26
    for index, polygon in enumerate(polygons):
        center_x = round(sum(point[0] for point in polygon) / len(polygon))
        center_y = round(sum(point[1] for point in polygon) / len(polygon))
        label_y = min(max(center_y, previous_y + 48), y + height - 22 - 48 * (len(polygons) - index - 1))
        draw.line((center_x, center_y, label_x - 19, label_y), fill=INK, width=5)
        draw.line((center_x, center_y, label_x - 19, label_y), fill=MARK, width=2)
        draw.ellipse((label_x - 19, label_y - 19, label_x + 19, label_y + 19), fill=MARK, outline=INK, width=2)
        draw.text((label_x, label_y), str(index + 1), fill=INK, font=font, anchor="mm")
        previous_y = label_y
    for polygon in polygons:
        _outline(draw, polygon, 3)


def write_pdf(output: Path, summary: dict, sample_images: list[SampleEvidence], font_path: str):
    title_font = ImageFont.truetype(font_path, 54)
    normal_font = ImageFont.truetype(font_path, 29)
    small_font = ImageFont.truetype(font_path, 24)
    size = (1240, 1754)
    foreground = INK
    pages = []

    def page(title):
        image = Image.new("RGB", size, "white")
        draw = ImageDraw.Draw(image)
        draw.rectangle((0, 0, size[0], 14), fill="#137e73")
        draw.text((83, 83), title, fill=foreground, font=title_font)
        draw.text((83, 1655), "风机叶片巡检  /  AI 检测结果", fill="#446370", font=small_font)
        draw.text((1120, 1655), str(len(pages) + 1), fill="#446370", font=small_font)
        pages.append(image)
        return image, draw

    def wrap(text, font, width=1060):
        line = ""
        lines = []
        for character in " ".join(text.splitlines()):
            if font.getlength(line + character) > width and line:
                lines.append(line)
                line = character
            else:
                line += character
        return [*lines, line]

    _, cover = page("风机叶片 AI 检测报告")
    header = [f"风场  {summary['farmName']}", f"风机编号  {summary['turbineNumber']}",
              f"任务编号  {summary['taskId']}", f"提交时间  {summary['createdAt']}",
              f"处理图片  {summary['imageCount']} 张    检出图片  {summary['defectImageCount']} 张",
              f"检出总数  {summary['detectionCount']} 处    模型  {summary['model']}"]
    y = 205
    for field in header:
        for line in wrap(field, normal_font):
            cover.text((85, y), line, fill=foreground, font=normal_font)
            y += 45
        y += 12
    for index, (label, count) in enumerate(summary["classes"].items()):
        x = 85 + (index % 2) * 540
        top = y + 30 + (index // 2) * 78
        cover.rectangle((x, top, x + 510, top + 64), fill="#eef5f4")
        cover.text((x + 22, top + 11), label, fill=foreground, font=normal_font)
        cover.text((x + 480, top + 11), str(count), fill=foreground, font=normal_font, anchor="rt")
    if sample_images:
        cover.text((85, y + 460), f"后附 {len(sample_images)} 张代表图，共 {summary['defectImageCount']} 张检出图。",
                   fill=foreground, font=normal_font)
    else:
        cover.text((85, y + 460), "本次任务无检出项。", fill=foreground, font=normal_font)

    for sample in sample_images:
        canvas, draw = page("缺陷定位与近照")
        title = wrap(sample.name, small_font, 1020)[0]
        draw.text((85, 188), title + ("…" if title != sample.name else ""), fill=foreground, font=small_font)
        draw.text((85, 233), f"整图检出 {sample.count} 处 · 展示 {len(sample.details)} 处近照",
                  fill=foreground, font=small_font)
        _draw_overview(canvas, draw, sample, small_font)
        for index, detail in enumerate(sample.details):
            x = 85 + (index % 2) * 550
            y = 938 + (index // 2) * 326
            draw.rounded_rectangle((x, y, x + 36, y + 34), radius=8, fill=MARK)
            draw.text((x + 18, y + 17), str(index + 1), fill=INK, font=small_font, anchor="mm")
            draw.text((x + 50, y + 2), f"{detail.detection.label}  ·  置信度 {detail.detection.confidence:.1%}",
                      fill=foreground, font=small_font)
            top = y + 47
            width, height = DETAIL_SIZE
            draw.rectangle((x, top, x + width, top + height), fill="#edf3f4")
            with Image.open(detail.path) as image:
                canvas.paste(image, (x + (width - image.width) // 2, top + (height - image.height) // 2))
    pages[0].save(output, "PDF", save_all=True, append_images=pages[1:], resolution=150)
