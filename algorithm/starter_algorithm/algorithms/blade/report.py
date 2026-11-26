from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


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


def write_pdf(output: Path, summary: dict, sample_images: list[tuple[str, Path]], font_path: str):
    title_font = ImageFont.truetype(font_path, 54)
    normal_font = ImageFont.truetype(font_path, 29)
    small_font = ImageFont.truetype(font_path, 24)
    size = (1240, 1754)
    foreground = "#173440"
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
        for character in text:
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
    if not sample_images:
        cover.text((85, y + 460), "本次任务无检出项。", fill=foreground, font=normal_font)

    for start in range(0, len(sample_images), 2):
        canvas, draw = page("代表性标注图")
        for index, (name, path) in enumerate(sample_images[start:start + 2]):
            top = 208 + index * 710
            title = wrap(name, small_font)[0]
            if title != name:
                title += "…"
            draw.text((85, top), title, fill=foreground, font=small_font)
            with Image.open(path) as evidence:
                evidence.thumbnail((1070, 590), Image.Resampling.LANCZOS)
                canvas.paste(evidence, ((1240 - evidence.width) // 2, top + 54))
    pages[0].save(output, "PDF", save_all=True, append_images=pages[1:], resolution=150)
