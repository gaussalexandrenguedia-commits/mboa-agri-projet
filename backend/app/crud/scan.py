from dataclasses import dataclass

from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.scan import Scan


@dataclass
class ScanSyncResult:
    scan: Scan
    status: str


def get_scan_by_user_and_local_id(
    db: Session, user_id: int, local_id: int
) -> Scan | None:
    statement = select(Scan).where(
        Scan.user_id == user_id,
        Scan.local_id == local_id,
    )
    return db.scalar(statement)


def create_or_get_scan(
    db: Session,
    *,
    user_id: int,
    local_id: int,
    plant_name: str,
    disease_name: str,
    confidence: int,
    symptoms: str,
    treatment_local: str,
    treatment_chemical: str,
    timestamp: int,
    pathology_id: int | None = None,
    commune_id: int | None = None,
    hors_ligne: bool = False,
    latitude: float | None = None,
    longitude: float | None = None,
) -> ScanSyncResult:
    """Insert a scan once; retries return the existing row."""
    existing = get_scan_by_user_and_local_id(db, user_id, local_id)
    if existing is not None:
        return ScanSyncResult(existing, "already_synced")

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
    try:
        db.commit()
    except IntegrityError:
        # Protect against two retry requests arriving concurrently.
        db.rollback()
        existing = get_scan_by_user_and_local_id(db, user_id, local_id)
        if existing is None:
            raise
        return ScanSyncResult(existing, "already_synced")
    db.refresh(scan)
    return ScanSyncResult(scan, "created")


def get_scan_by_id(db: Session, scan_id: int, user_id: int) -> Scan | None:
    statement = select(Scan).where(Scan.id == scan_id, Scan.user_id == user_id)
    return db.scalar(statement)


def get_scans(db: Session, user_id: int, limit: int = 50) -> list[Scan]:
    statement = (
        select(Scan)
        .where(Scan.user_id == user_id)
        .order_by(Scan.received_at.desc())
        .limit(limit)
    )
    return list(db.scalars(statement).all())
