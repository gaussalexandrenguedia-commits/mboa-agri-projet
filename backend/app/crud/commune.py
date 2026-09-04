from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.commune import Commune


def get_all_communes(db: Session) -> list[Commune]:
    statement = select(Commune).order_by(Commune.name.asc())
    return list(db.scalars(statement).all())


def get_commune_by_id(db: Session, commune_id: int) -> Commune | None:
    statement = select(Commune).where(Commune.id == commune_id)
    return db.scalar(statement)


def get_commune_by_name(db: Session, name: str) -> Commune | None:
    statement = select(Commune).where(Commune.name.ilike(name.strip()))
    return db.scalar(statement)


def update_commune(
    db: Session,
    commune: Commune,
    name: str | None = None,
    postal_code: str | None = None,
) -> Commune:
    if name is not None:
        commune.name = name.strip()

    if postal_code is not None:
        commune.postal_code = postal_code.strip()

    db.commit()
    db.refresh(commune)
    return commune
