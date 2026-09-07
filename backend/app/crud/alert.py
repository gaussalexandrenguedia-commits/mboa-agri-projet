from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.commune import Commune
from app.models.territorial_alert import TerritorialAlert
from app.crud.commune import get_commune_by_name
from app.models.territorial_alert import TerritorialAlert



def get_alerts_by_commune(
    db: Session,
    commune_id: int,
) -> list[TerritorialAlert]:
    statement = (
        select(TerritorialAlert)
        .where(TerritorialAlert.commune_id == commune_id)
        .order_by(TerritorialAlert.created_at.desc())
    )
    return list(db.scalars(statement).all())


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