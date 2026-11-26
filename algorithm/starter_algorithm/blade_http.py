import os
from contextlib import asynccontextmanager
from pathlib import Path
from threading import Lock
from uuid import UUID

from fastapi import FastAPI, HTTPException, Response
from pydantic import BaseModel, Field

from .algorithms.blade.archive import InvalidArchive, inspect_archive
from .algorithms.blade.pipeline import Predictor


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.provider = os.getenv("BLADE_PROVIDER", "CUDAExecutionProvider")
    app.state.predictor = Predictor(Path(os.environ["BLADE_MODEL_PATH"]), app.state.provider,
                                    float(os.getenv("BLADE_CONFIDENCE", "0.25")))
    app.state.lock = Lock()
    yield


app = FastAPI(title="Blade inspection worker", lifespan=lifespan, docs_url=None, redoc_url=None, openapi_url=None)


class Inspection(BaseModel):
    taskId: UUID
    attemptId: UUID
    farmName: str = Field(min_length=1, max_length=80)
    turbineNumber: str = Field(min_length=1, max_length=60)
    originalName: str = Field(min_length=1, max_length=255)
    createdAt: str = Field(min_length=1, max_length=60)


@app.get("/health")
def health():
    return {"status": "UP", "provider": app.state.provider}


@app.post("/blade/inspect", status_code=204)
def inspect(task: Inspection):
    try:
        with app.state.lock:
            inspect_archive(task.model_dump(mode="json"), Path(os.environ["BLADE_DATA_DIR"]), app.state.predictor,
                            os.environ["BLADE_FONT_PATH"], int(os.getenv("BLADE_MAX_EXPANDED_BYTES", "30000000000")))
    except InvalidArchive as failure:
        raise HTTPException(status_code=422, detail="Invalid inspection archive") from failure
    return Response(status_code=204)
