from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.pathology import Pathology
from app.schemas.pathology import (
    PathologyCreateRequest,
    PathologyUpdateRequest,
)


def get_pathologies(
    db: Session,
    *,
    crop_name: str | None = None,
    is_active: bool | None = None,
) -> list[Pathology]:
    statement = select(Pathology)

    if crop_name is not None:
        statement = statement.where(
            Pathology.crop_name.ilike(
                crop_name.strip()
            )
        )

    if is_active is not None:
        statement = statement.where(
            Pathology.is_active.is_(is_active)
        )

    statement = statement.order_by(
        Pathology.common_name.asc()
    )

    return list(
        db.scalars(statement).all()
    )


def get_pathology_by_id(
    db: Session,
    pathology_id: int,
) -> Pathology | None:
    return db.get(
        Pathology,
        pathology_id,
    )


def get_pathology_by_code(
    db: Session,
    code: str,
) -> Pathology | None:
    statement = select(Pathology).where(
        Pathology.code == code.strip()
    )

    return db.scalar(statement)


def create_pathology(
    db: Session,
    payload: PathologyCreateRequest,
) -> Pathology:
    values = payload.model_dump()

    values["code"] = payload.code.strip()

    values["common_name"] = (
        payload.common_name.strip()
    )

    values["crop_name"] = (
        payload.crop_name.strip()
    )

    values["technical_name"] = (
        payload.technical_name.strip()
        if payload.technical_name
        else None
    )

    pathology = Pathology(
        **values,
    )

    db.add(pathology)
    db.commit()
    db.refresh(pathology)

    return pathology


def update_pathology(
    db: Session,
    pathology: Pathology,
    payload: PathologyUpdateRequest,
) -> Pathology:
    values = payload.model_dump(
        exclude_unset=True
    )

    text_fields = (
        "code",
        "technical_name",
        "common_name",
        "crop_name",
        "key_symptoms",
        "biological_treatment",
        "chemical_treatment",
        "default_severity",
    )

    for field in text_fields:
        if (
            field in values
            and isinstance(values[field], str)
        ):
            values[field] = values[field].strip()

    for field, value in values.items():
        setattr(
            pathology,
            field,
            value,
        )

    db.commit()
    db.refresh(pathology)

    return pathology
