FROM python:3.12-slim
COPY --from=ghcr.io/astral-sh/uv:0.12.15 /uv /bin/uv
ARG TRANSPORT=http
ENV PYTHONUNBUFFERED=1
WORKDIR /app
COPY pyproject.toml uv.lock ./
RUN uv sync --locked --extra "$TRANSPORT"
COPY starter_algorithm ./starter_algorithm
ENV PATH="/app/.venv/bin:$PATH"
CMD ["uvicorn", "starter_algorithm.http:app", "--host", "0.0.0.0", "--port", "8000"]
