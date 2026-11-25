from uuid import UUID

from fastapi import FastAPI
from pydantic import BaseModel, Field

from .engine import detect

app = FastAPI(title="Detection algorithm")


class DetectionRequest(BaseModel):
    id: UUID
    sample: str = Field(min_length=1, max_length=120)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.post("/detect")
def submit(request: DetectionRequest) -> dict:
    return {"id": str(request.id), "result": detect(request.sample)}
