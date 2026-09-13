import time

from fastapi import (
    APIRouter,
    Depends,
    File,
    Form,
    HTTPException,
    UploadFile,
    status,
)
from sqlalchemy.orm import Session

from app.core.dependencies import get_current_user
from app.crud.commune import get_commune_by_code
from app.crud.diagnostic import create_diagnostic
from app.database import get_db
from app.models.user import User
from app.schemas.diagnostic import DiagnosticResponse
from app.services.diagnostic import (
    get_catalog_for_plant,
    resolve_prediction,
)
from app.services.gemini import (
    GeminiService,
    GeminiServiceError,
)


router = APIRouter(
    prefix="/api/scans",
    tags=["Diagnostics"],
)


ALLOWED_IMAGE_TYPES = {
    "image/jpeg",
    "image/png",
    "image/webp",
}

MAX_IMAGE_SIZE = 10 * 1024 * 1024


@router.post(
    "/diagnose",
    response_model=DiagnosticResponse,
)
async def diagnose_scan(
    image: UploadFile = File(...),
    plant_name: str = Form(
        ...,
        min_length=1,
        max_length=100,
    ),
    symptoms: str = Form(default=""),
    local_id: int | None = Form(
        default=None,
        ge=0,
    ),
    commune_code: str | None = Form(
        default=None,
        max_length=50,
    ),
    latitude: float | None = Form(
        default=None,
        ge=-90,
        le=90,
    ),
    longitude: float | None = Form(
        default=None,
        ge=-180,
        le=180,
    ),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> DiagnosticResponse:
    if image.content_type not in ALLOWED_IMAGE_TYPES:
        raise HTTPException(
            status_code=(
                status.HTTP_415_UNSUPPORTED_MEDIA_TYPE
            ),
            detail=(
                "Format image accepté : "
                "JPEG, PNG ou WEBP."
            ),
        )

    image_bytes = await image.read()

    if (
        not image_bytes
        or len(image_bytes) > MAX_IMAGE_SIZE
    ):
        raise HTTPException(
            status_code=(
                status.HTTP_413_REQUEST_ENTITY_TOO_LARGE
            ),
            detail=(
                "L’image est vide ou dépasse 10 Mo."
            ),
        )

    commune_id = None

    if commune_code:
        commune = get_commune_by_code(
            db,
            commune_code,
        )

        if commune is None:
            raise HTTPException(
                status_code=(
                    status.HTTP_422_UNPROCESSABLE_ENTITY
                ),
                detail=(
                    f"Code commune inconnu : "
                    f"{commune_code}"
                ),
            )

        commune_id = commune.id

    try:
        service = GeminiService()

        prediction = service.diagnose(
            image_bytes=image_bytes,
            mime_type=image.content_type,
            plant_name=plant_name.strip(),
            symptoms=symptoms.strip(),
            pathology_catalog=(
                get_catalog_for_plant(
                    db,
                    plant_name,
                )
            ),
        )

        resolution = resolve_prediction(
            db,
            prediction,
            plant_name,
        )

        diagnostic = create_diagnostic(
            db=db,
            user_id=current_user.id,
            local_id=local_id,
            pathology_id=resolution.pathology_id,
            commune_id=commune_id,
            plant_name=plant_name.strip(),
            disease_name=resolution.disease_name,
            confidence=prediction.confidence,
            symptoms=resolution.symptoms,
            treatment_local=resolution.treatment_local,
            treatment_chemical=resolution.treatment_chemical,
            timestamp=int(time.time() * 1000),
            hors_ligne=False,
            latitude=latitude,
            longitude=longitude,
            image_url=None,
            severity_detected=(
                resolution.severity_detected
            ),
            severity_default=(
                resolution.severity_default
            ),
            validation_status=(
                resolution.validation_status
            ),
        )

        db.commit()
        db.refresh(diagnostic)

    except GeminiServiceError as exc:
        db.rollback()

        raise HTTPException(
            status_code=(
                status.HTTP_503_SERVICE_UNAVAILABLE
            ),
            detail=str(exc),
        ) from exc

    except Exception:
        db.rollback()
        raise

    return DiagnosticResponse(
        id=diagnostic.id,
        local_id=diagnostic.local_id,
        plant_name=diagnostic.plant_name,
        disease_name=diagnostic.disease_name,
        confidence=diagnostic.confidence,
        symptoms=diagnostic.symptoms,
        treatment_local=diagnostic.treatment_local,
        treatment_chemical=diagnostic.treatment_chemical,
        pathology_id=diagnostic.pathology_id,
        pathology_code=resolution.pathology_code,
        commune_id=diagnostic.commune_id,
        commune_code=commune_code,
        latitude=(
            float(diagnostic.latitude)
            if diagnostic.latitude is not None
            else None
        ),
        longitude=(
            float(diagnostic.longitude)
            if diagnostic.longitude is not None
            else None
        ),
        image_url=diagnostic.image_url,
        hors_ligne=diagnostic.hors_ligne,
        severity_detected=(
            diagnostic.severity_detected
        ),
        severity_default=(
            diagnostic.severity_default
        ),
        information_source=(
            resolution.information_source
        ),
        validation_status=(
            diagnostic.validation_status
        ),
    )
