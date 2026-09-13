from fastapi import APIRouter, Depends, HTTPException, Query, Response, status
from sqlalchemy.orm import Session

from app.core.dependencies import get_current_user
from app.crud.scan import create_or_get_scan, get_scan_by_id, get_scans
from app.database import get_db
from app.models.user import User
from app.schemas.scan import ScanCreateRequest, ScanResponse


router = APIRouter(
    prefix="/api/scans",
    tags=["Scans"],
)


@router.post(
    "",
    response_model=ScanResponse,
    status_code=status.HTTP_201_CREATED,
)
def upload_scan(
    data: ScanCreateRequest,
    response: Response,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> ScanResponse:

    result = create_or_get_scan(
        db=db,
        user_id=current_user.id,
        local_id=data.local_id,
        pathology_id=data.pathology_id,
        commune_id=data.commune_id,
        plant_name=data.plant_name.strip(),
        disease_name=data.disease_name.strip(),
        confidence=data.confidence,
        symptoms=data.symptoms.strip(),
        treatment_local=data.treatment_local.strip(),
        treatment_chemical=data.treatment_chemical.strip(),
        timestamp=data.timestamp,
        hors_ligne=data.hors_ligne,
        latitude=data.latitude,
        longitude=data.longitude,
    )

    if result.status == "already_synced":
        response.status_code = status.HTTP_200_OK

    payload = ScanResponse.model_validate(result.scan)

    payload.sync_status = result.status
    payload.message = (
        "Scan déjà synchronisé."
        if result.status == "already_synced"
        else "Scan synchronisé avec succès."
    )

    return payload


@router.get(
    "/{scan_id}",
    response_model=ScanResponse,
)
def read_scan(
    scan_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> ScanResponse:
    """
    Retourne un scan appartenant uniquement à l'utilisateur connecté.
    """

    scan = get_scan_by_id(
        db=db,
        scan_id=scan_id,
        user_id=current_user.id,
    )

    if scan is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Scan introuvable.",
        )

    return ScanResponse.model_validate(scan)


@router.get(
    "",
    response_model=list[ScanResponse],
)
def read_scans(
    limit: int = Query(
        default=50,
        ge=1,
        le=100,
    ),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[ScanResponse]:
    """
    Retourne uniquement les scans de l'utilisateur connecté.
    """

    scans = get_scans(
        db=db,
        user_id=current_user.id,
        limit=limit,
    )

    return [
        ScanResponse.model_validate(scan)
        for scan in scans
    ]
