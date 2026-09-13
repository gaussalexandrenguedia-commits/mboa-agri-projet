from datetime import datetime

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.commune import Commune
from app.models.pathology import Pathology
from app.models.territorial_alert import TerritorialAlert


def get_alerts(
    db: Session,
    *,
    commune_id: int | None = None,
    commune_code: str | None = None,
    pathology_id: int | None = None,
    crop_name: str | None = None,
    start_date: datetime | None = None,
    end_date: datetime | None = None,
) -> list[TerritorialAlert]:
    statement = select(TerritorialAlert)

    if commune_id is not None:
        statement = statement.where(
            TerritorialAlert.commune_id == commune_id
        )

    if commune_code is not None:
        statement = statement.join(
            Commune,
            Commune.id == TerritorialAlert.commune_id,
        ).where(
            Commune.code == commune_code.strip()
        )

    if pathology_id is not None:
        statement = statement.where(
            TerritorialAlert.pathology_id == pathology_id
        )

    if crop_name is not None:
        statement = statement.join(
            Pathology,
            Pathology.id == TerritorialAlert.pathology_id,
        ).where(
            Pathology.crop_name.ilike(crop_name.strip())
        )

    if start_date is not None:
        statement = statement.where(
            TerritorialAlert.created_at >= start_date
        )

    if end_date is not None:
        statement = statement.where(
            TerritorialAlert.created_at <= end_date
        )

    statement = statement.order_by(
        TerritorialAlert.created_at.desc()
    )

    return list(
        db.scalars(statement).unique().all()
    )


def get_alerts_by_commune(
    db: Session,
    commune_id: int,
) -> list[TerritorialAlert]:
    return get_alerts(
        db,
        commune_id=commune_id,
    )


def create_alert(
    db: Session,
    pathology_id: int,
    commune_id: int,
    scan_count: int,
    alert_level: str,
) -> TerritorialAlert:
    alert = TerritorialAlert(
        pathology_id=pathology_id,
        commune_id=commune_id,
        scan_count=scan_count,
        alert_level=alert_level.strip(),
    )

    db.add(alert)
    db.commit()
    db.refresh(alert)

    return alert
