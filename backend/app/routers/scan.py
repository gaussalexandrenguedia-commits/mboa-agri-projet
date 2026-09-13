from fastapi import (
    APIRouter,
    Depends,
    HTTPException,
    Query,
    Response,
    status,
)
from sqlalchemy.orm import Session

from app.core.dependencies import get_current_user
from app.crud.commune import get_commune_by_code
from app.crud.scan import (
    create_or_get_scan,
    get_scan_by_id,
    get_scans,
)
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
    """
    Reçoit un scan mobile et l'associe à l'utilisateur du JWT.

    data.user_id est un identifiant local Room.
    Il n'est jamais utilisé pour l'authentification.

    data.commune_id est également un identifiant local Room.
    La commune PostgreSQL est résolue avec commune_code.
    """

    commune_id = None

    if data.commune_code is not None:
        commune = get_commune_by_code(
            db,
            data.commune_code,
        )

        if commune is None:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail=(
                    f"Code commune inconnu : "
                    f"{data.commune_code}"
                ),
            )

        commune_id = commune.id

    result = create_or_get_scan(
        db=db,
        user_id=current_user.id,
        local_id=data.local_id,
        pathology_id=data.pathology_id,
        commune_id=commune_id,
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

    payload = ScanResponse.model_validate(
        result.scan
    )

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
    scans = get_scans(
        db=db,
        user_id=current_user.id,
        limit=limit,
    )

    return [
        ScanResponse.model_validate(scan)
        for scan in scans
    ]
