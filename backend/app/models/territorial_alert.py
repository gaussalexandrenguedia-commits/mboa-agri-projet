from datetime import datetime, timezone

from sqlalchemy import DateTime, ForeignKey, Integer, String, Index
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class TerritorialAlert(Base):
    __tablename__ = "alerts"
    __table_args__ = (
        Index(
            "ix_alerts_commune_pathology_status",
            "commune_id",
            "pathology_id",
            "status",
        ),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    pathology_id: Mapped[int] = mapped_column(
        ForeignKey("pathologies.id"),
        nullable=False,
    )
    commune_id: Mapped[int] = mapped_column(
        ForeignKey("communes.id"),
        nullable=False,
    )
    scan_count: Mapped[int] = mapped_column(Integer, nullable=False)
    alert_level: Mapped[str] = mapped_column(String(20), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
    )
    # Enrichissement cycle de vie (recommandé rapport)
    period_start: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    period_end: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    last_scan_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default="ACTIVE"
    )
    resolved_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
