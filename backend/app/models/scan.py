from datetime import datetime, timezone

from sqlalchemy import DateTime, Float, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class Scan(Base):
    __tablename__ = "scans"

    id: Mapped[int] = mapped_column(primary_key=True)
    local_id: Mapped[int] = mapped_column(Integer, nullable=False)
    plant_name: Mapped[str] = mapped_column(String(100), nullable=False)
    disease_name: Mapped[str] = mapped_column(String(150), nullable=False)
    confidence: Mapped[int] = mapped_column(Integer, nullable=False)
    symptoms: Mapped[str] = mapped_column(Text, nullable=False)
    treatment_local: Mapped[str] = mapped_column(Text, nullable=False)
    treatment_chemical: Mapped[str] = mapped_column(Text, nullable=False)
    timestamp: Mapped[int] = mapped_column(Integer, nullable=False)
    latitude: Mapped[float | None] = mapped_column(Float, nullable=True)
    longitude: Mapped[float | None] = mapped_column(Float, nullable=True)
    received_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
    )
