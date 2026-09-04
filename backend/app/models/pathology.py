from sqlalchemy import Boolean, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class Pathology(Base):
    __tablename__ = "pathologies"

    id: Mapped[int] = mapped_column(primary_key=True)
    technical_name: Mapped[str | None] = mapped_column(String(100), nullable=True)
    common_name: Mapped[str] = mapped_column(String(100), nullable=False)
    crop_name: Mapped[str] = mapped_column(String(50), nullable=False)
    key_symptoms: Mapped[str | None] = mapped_column(Text, nullable=True)
    biological_treatment: Mapped[str | None] = mapped_column(Text, nullable=True)
    chemical_treatment: Mapped[str | None] = mapped_column(Text, nullable=True)
    default_severity: Mapped[str] = mapped_column(
        String(20),
        nullable=False,
        default="Attention",
    )
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
