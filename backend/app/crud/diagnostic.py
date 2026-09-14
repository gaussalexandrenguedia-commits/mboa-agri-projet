from geoalchemy2.elements import WKTElement
from sqlalchemy.orm import Session

from app.models.diagnostic import Diagnostic


def create_diagnostic(
    db: Session,
    *,
    user_id: int | None,
    local_id: int | None,
    pathology_id: int | None,
    commune_id: int | None,
    plant_name: str,
    disease_name: str,
    confidence: int,
    symptoms: str | None,
    treatment_local: str | None,
    treatment_chemical: str | None,
    timestamp: int,
    hors_ligne: bool,
    latitude: float | None,
    longitude: float | None,
    image_url: str | None,
    severity_detected: str | None,
    severity_default: str | None,
    validation_status: str,
) -> Diagnostic:
    position_gps = None

    if (
        latitude is not None
        and longitude is not None
    ):
        position_gps = WKTElement(
            f"POINT({longitude} {latitude})",
            srid=4326,
        )

    diagnostic = Diagnostic(
        user_id=user_id,
        local_id=local_id,
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
        position_gps=position_gps,
        image_url=image_url,
        severity_detected=severity_detected,
        severity_default=severity_default,
        validation_status=validation_status,
    )

    db.add(diagnostic)
    db.flush()

    return diagnostic
