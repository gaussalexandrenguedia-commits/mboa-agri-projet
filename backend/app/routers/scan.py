from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.crud.scan import create_scan, get_scan_by_id, get_scans
from app.database import get_db
from app.schemas.scan import ScanCreateRequest, ScanResponse


from sqlalchemy.orm import Session


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
    db: Session = Depends(get_db),
) -> ScanResponse:
    scan = create_scan(
        db=db,
        local_id=data.local_id,
        plant_name=data.plant_name,
        disease_name=data.disease_name,
        confidence=data.confidence,
        symptoms=data.symptoms,
        treatment_local=data.treatment_local,
        treatment_chemical=data.treatment_chemical,
        timestamp=data.timestamp,
        latitude=data.latitude,
        longitude=data.longitude,
    )

    return ScanResponse.model_validate(scan)


@router.get("/{scan_id}", response_model=ScanResponse)
def read_scan(
    scan_id: int,
    db: Session = Depends(get_db),
) -> ScanResponse:
    scan = get_scan_by_id(db, scan_id)

    if scan is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Scan introuvable.",
        )

    return ScanResponse.model_validate(scan)


@router.get("", response_model=list[ScanResponse])
def read_scans(
    limit: int = Query(default=50, ge=1, le=100),
    db: Session = Depends(get_db),
) -> list[ScanResponse]:
    scans = get_scans(db, limit=limit)
    return [ScanResponse.model_validate(scan) for scan in scans]
