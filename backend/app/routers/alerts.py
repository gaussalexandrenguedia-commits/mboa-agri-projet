from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.crud.alert import create_alert, get_alerts
from app.crud.commune import get_commune_by_id, get_commune_by_name
from app.database import get_db
from app.models.pathology import Pathology
from app.schemas.alert import AlertCreateRequest, AlertResponse


router = APIRouter(
    prefix="/alerts",
    tags=["Alerts"],
)


@router.get(
    "",
    response_model=list[AlertResponse],
)
def read_alerts(
    commune: str | None = Query(
        default=None,
        min_length=1,
    ),
    commune_code: str | None = Query(
        default=None,
        min_length=1,
    ),
    pathology_id: int | None = Query(
        default=None,
        ge=1,
    ),
    crop_name: str | None = Query(
        default=None,
        min_length=1,
    ),
    start_date: datetime | None = Query(
        default=None,
    ),
    end_date: datetime | None = Query(
        default=None,
    ),
    db: Session = Depends(get_db),
) -> list[AlertResponse]:
    if (
        start_date is not None
        and end_date is not None
        and start_date > end_date
    ):
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=(
                "start_date doit être antérieure "
                "ou égale à end_date."
            ),
        )

    commune_id = None

    # Ancienne compatibilité avec GET /alerts?commune=Nom
    if commune is not None:
        commune_record = get_commune_by_name(
            db,
            commune,
        )

        if commune_record is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Commune introuvable.",
            )

        commune_id = commune_record.id

    alerts = get_alerts(
        db=db,
        commune_id=commune_id,
        commune_code=commune_code,
        pathology_id=pathology_id,
        crop_name=crop_name,
        start_date=start_date,
        end_date=end_date,
    )

    return [
        AlertResponse.model_validate(alert)
        for alert in alerts
    ]


@router.post(
    "",
    response_model=AlertResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_new_alert(
    payload: AlertCreateRequest,
    db: Session = Depends(get_db),
) -> AlertResponse:
    commune = get_commune_by_id(
        db,
        payload.commune_id,
    )

    if commune is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Commune introuvable.",
        )

    pathology = db.get(
        Pathology,
        payload.pathology_id,
    )

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
