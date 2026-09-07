from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class Commune(Base):
    __tablename__ = "communes"

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(100), unique=True, index=True)
    postal_code: Mapped[str | None] = mapped_column(String(20), nullable=True)
