from datetime import datetime, timezone

from sqlalchemy import DateTime, Float, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class PathologyCandidate(Base):
    __tablename__ = "pathology_candidates"

    id: Mapped[int] = mapped_column(
        primary_key=True,
    )

    proposed_code: Mapped[str] = mapped_column(
        String(100),
        unique=True,
        index=True,
        nullable=False,
    )

    proposed_name: Mapped[str] = mapped_column(
        String(150),
        nullable=False,
    )

    plant_name: Mapped[str] = mapped_column(
        String(100),
        nullable=False,
    )

    symptoms_ai: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
    )

    biological_treatment_ai: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
    )

    chemical_treatment_ai: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
    )

    severity_detected: Mapped[str | None] = mapped_column(
        String(20),
        nullable=True,
    )

    confidence: Mapped[float | None] = mapped_column(
        Float,
        nullable=True,
    )

    first_seen_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
    )

    last_seen_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
    )

    occurrence_count: Mapped[int] = mapped_column(
        Integer,
        nullable=False,
        default=1,
    )

    status: Mapped[str] = mapped_column(
        String(20),
        nullable=False,
        default="PENDING_REVIEW",
    )

    review_comment: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
    )

    validated_pathology_id: Mapped[int | None] = mapped_column(
        Integer,
        nullable=True,
    )
