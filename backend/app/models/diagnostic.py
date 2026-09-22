from datetime import datetime, timezone

from geoalchemy2 import Geography
from sqlalchemy import (
    BigInteger,
    Boolean,
    DateTime,
    ForeignKey,
    Integer,
    Numeric,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class Diagnostic(Base):
    __tablename__ = "diagnostics"
    __table_args__ = (
        UniqueConstraint(
            "user_id", "local_id", "hors_ligne",
            name="uq_diagnostics_user_local_hors_ligne"
        ),
    )

    id: Mapped[int] = mapped_column(
        primary_key=True,
    )

    user_id: Mapped[int | None] = mapped_column(
        ForeignKey("users.id"),
        nullable=True,
    )

    pathology_id: Mapped[int | None] = mapped_column(
        ForeignKey("pathologies.id"),
        nullable=True,
    )

    commune_id: Mapped[int | None] = mapped_column(
        ForeignKey("communes.id"),
        nullable=True,
    )

    local_id: Mapped[int | None] = mapped_column(
        Integer,
        nullable=True,
    )

    plant_name: Mapped[str] = mapped_column(
        String(100),
        nullable=False,
    )

    disease_name: Mapped[str] = mapped_column(
        String(150),
        nullable=False,
    )

    confidence: Mapped[int] = mapped_column(
        Integer,
        nullable=False,
    )

    symptoms: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
    )

    treatment_local: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
    )

    treatment_chemical: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
    )

    timestamp: Mapped[int] = mapped_column(
        BigInteger,
        nullable=False,
    )

    hors_ligne: Mapped[bool] = mapped_column(
        Boolean,
        nullable=False,
        default=False,
    )

    latitude: Mapped[float | None] = mapped_column(
        Numeric(9, 6),
        nullable=True,
    )

    longitude: Mapped[float | None] = mapped_column(
        Numeric(9, 6),
        nullable=True,
    )

    position_gps = mapped_column(
        Geography(
            geometry_type="POINT",
            srid=4326,
            spatial_index=True,
        ),
        nullable=True,
    )

    image_url: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
    )

    severity_detected: Mapped[str | None] = mapped_column(
        String(20),
        nullable=True,
    )

    severity_default: Mapped[str | None] = mapped_column(
        String(20),
        nullable=True,
    )

    validation_status: Mapped[str] = mapped_column(
        String(30),
        nullable=False,
        default="PENDING_REVIEW",
    )

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
    )
