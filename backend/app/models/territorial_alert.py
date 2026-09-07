from datetime import datetime, timezone

from sqlalchemy import DateTime, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class TerritorialAlert(Base):
    __tablename__ = "alerts"

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
