from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.scan import Scan


def create_scan(
    db: Session,
    local_id: int,
    plant_name: str,
    disease_name: str,
    confidence: int,
    symptoms: str,
    treatment_local: str,
    treatment_chemical: str,
    timestamp: int,
    user_id: int | None = None,
    pathology_id: int | None = None,
    commune_id: int | None = None,
    hors_ligne: bool = False,
    latitude: float | None = None,
    longitude: float | None = None,
    ) -> Scan:
    scan = Scan(
    local_id=local_id,
    user_id=user_id,
    pathology_id=pathology_id,
    commune_id=commune_id,
    plant_name=plant_name,
    disease_name=disease_name,
    confidence=confidence,
    symptoms=symptoms,
    treatment_local=treatment_local,
    treatment_chemical=treatment_chemical,
    timestamp=timestamp,
    hors_ligne=hors_ligne,
    latitude=latitude,
    longitude=longitude,
    )
    db.add(scan)
    db.commit()
    db.refresh(scan)
    return scan


def get_scan_by_id(db: Session, scan_id: int) -> Scan | None:
    statement = select(Scan).where(Scan.id == scan_id)
    return db.scalar(statement)


def get_scans(db: Session, limit: int = 50) -> list[Scan]:
    statement = select(Scan).order_by(Scan.received_at.desc()).limit(limit)
    return list(db.scalars(statement).all())
