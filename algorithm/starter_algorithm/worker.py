import json
import logging
import os
from uuid import UUID

from kafka import KafkaConsumer, KafkaProducer

from .engine import detect

REQUESTS = "detection.jobs.v1"
RESULTS = "detection.results.v1"
LOGGER = logging.getLogger(__name__)


def main() -> None:
    logging.basicConfig(level=logging.INFO)
    bootstrap_servers = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "127.0.0.1:9092")
    consumer = KafkaConsumer(
        REQUESTS,
        bootstrap_servers=bootstrap_servers,
        group_id="detection-algorithm-v1",
        auto_offset_reset="earliest",
        enable_auto_commit=False,
    )
    producer = KafkaProducer(bootstrap_servers=bootstrap_servers, acks="all")
    try:
        LOGGER.info("Detection worker started")
        for message in consumer:
            try:
                request = json.loads(message.value)
                job_id = str(UUID(request["id"]))
                sample = request["sample"]
                if not isinstance(sample, str) or not 1 <= len(sample) <= 120:
                    raise ValueError("Invalid sample")
            except (TypeError, ValueError, KeyError):
                LOGGER.warning("Skipped invalid request at offset %s", message.offset)
                consumer.commit()
                continue

            result = json.dumps({"id": job_id, "result": detect(sample)}).encode("utf-8")
            producer.send(RESULTS, key=job_id.encode("ascii"), value=result).get(timeout=10)
            consumer.commit()
    finally:
        producer.close()
        consumer.close()


if __name__ == "__main__":
    main()
