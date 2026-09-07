from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.crud.alert import create_alert, get_alerts_by_commune
from app.crud.commune import get_commune_by_id, get_commune_by_name
from app.database import get_db
from app.models.pathology import Pathology
from app.schemas.alert import AlertCreateRequest, AlertResponse


router = APIRouter(
    prefix="/alerts",
    tags=["Alerts"],
)


@router.get("", response_model=list[AlertResponse])
def read_alerts(
    commune: str = Query(min_length=1),
    db: Session = Depends(get_db),
) -> list[AlertResponse]:
    commune_record = get_commune_by_name(db, commune)

    if commune_record is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Commune introuvable.",
        )

    alerts = get_alerts_by_commune(db, commune_record.id)
    return [AlertResponse.model_validate(alert) for alert in alerts]


@router.post(
    "",
    response_model=AlertResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_new_alert(
    payload: AlertCreateRequest,
    db: Session = Depends(get_db),
) -> AlertResponse:
    commune = get_commune_by_id(db, payload.commune_id)

    if commune is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Commune introuvable.",
        )

    pathology = db.get(Pathology, payload.pathology_id)

    if pathology is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Pathologie introuvable.",
        )

    alert = create_alert(
        db=db,
        pathology_id=payload.pathology_id,
        commune_id=payload.commune_id,
        scan_count=payload.scan_count,
        alert_level=payload.alert_level,
    )

    return AlertResponse.model_validate(alert)
