"""Export a built blade GPU stack as a self-contained linux/amd64 deployment archive."""

import argparse
import io
import json
import os
from pathlib import Path
import re
import subprocess
import tarfile
from uuid import uuid4


ROOT = Path(__file__).resolve().parents[2]
PLATFORM = "linux/amd64"
MODEL_NAME = "blade_defect_v1.1.onnx"
SERVICES = ("postgres", "algorithm", "backend", "vue")


def docker(*args):
    subprocess.run(["docker", *args], check=True)


def package(version, source_project, model, output):
    stem = f"blade-inspection-{version}-linux-amd64"
    output.mkdir(parents=True, exist_ok=True)
    destination = output / f"{stem}.tar.gz"
    if destination.exists():
        raise FileExistsError(destination)

    source = {name: f"{source_project}-{name}:latest" for name in SERVICES}
    source["postgres"] = "postgres:17-alpine"
    images = {name: f"blade-inspection/{name}:{version}" for name in SERVICES}
    records = json.loads(subprocess.check_output(["docker", "image", "inspect", *source.values()]))
    for image, record in zip(source.values(), records, strict=True):
        if f"{record['Os']}/{record['Architecture']}" != PLATFORM:
            raise ValueError(f"{image} does not match {PLATFORM}")
    docker("run", "--rm", "--network", "none", "--entrypoint", "python", records[1]["Id"], "-c",
           "import onnxruntime as ort; assert 'CUDAExecutionProvider' in ort.get_available_providers()")

    migrations = sorted((ROOT / "database/postgres/migrations").glob("V*__*.sql"))
    if not migrations:
        raise FileNotFoundError("Missing PostgreSQL migrations")
    files = {
        "compose.blade.yaml": ROOT / "compose.blade.yaml",
        "compose.blade.gpu.yaml": ROOT / "compose.blade.gpu.yaml",
        "deploy.sh": ROOT / "docker/blade-offline/deploy.sh",
        "README.md": ROOT / "docker/blade-offline/README.md",
        f"blade-models/{MODEL_NAME}": model,
        **{path.relative_to(ROOT).as_posix(): path for path in migrations},
    }
    for path in files.values():
        if not path.is_file() or path.stat().st_size == 0:
            raise FileNotFoundError(path)

    temporary = output / f".{stem}-{uuid4().hex}"
    image_archive = temporary.with_suffix(".images.tar")
    compressed = temporary.with_suffix(".tar.gz.part")
    try:
        for name, record in zip(SERVICES, records, strict=True):
            docker("image", "tag", record["Id"], images[name])
        print("Exporting Docker images", flush=True)
        docker("image", "save", "--platform", PLATFORM, "--output", str(image_archive), *images.values())

        image_config = "services:\n"
        for service, image in images.items():
            image_config += f"  {service}:\n    build: !reset null\n    image: {image}\n"
        config_bytes = image_config.encode("utf-8")
        print("Compressing deployment bundle", flush=True)
        with tarfile.open(compressed, "w:gz", compresslevel=1) as archive:
            for name, path in {**files, "images.tar": image_archive}.items():
                member = tarfile.TarInfo(f"{stem}/{name}")
                member.size = path.stat().st_size
                member.mode = 0o755 if name == "deploy.sh" else 0o644
                with path.open("rb") as handle:
                    archive.addfile(member, handle)
            member = tarfile.TarInfo(f"{stem}/compose.images.yaml")
            member.size = len(config_bytes)
            member.mode = 0o644
            archive.addfile(member, io.BytesIO(config_bytes))
        os.replace(compressed, destination)
    finally:
        image_archive.unlink(missing_ok=True)
        compressed.unlink(missing_ok=True)
    print(destination)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", required=True)
    parser.add_argument("--model", type=Path, required=True)
    parser.add_argument("--source-project", default="blade-inspection")
    parser.add_argument("--output", type=Path, default=ROOT / "dist/blade-offline")
    args = parser.parse_args()
    if not re.fullmatch(r"[a-z0-9][a-z0-9-]{0,63}", args.version):
        parser.error("--version must contain lowercase letters, digits and hyphens, up to 64 characters")
    package(args.version, args.source_project, args.model.resolve(), args.output.resolve())


if __name__ == "__main__":
    main()
