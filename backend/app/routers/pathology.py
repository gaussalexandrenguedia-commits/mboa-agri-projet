from fastapi import (
    APIRouter,
    Depends,
    HTTPException,
    Query,
    status,
)
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.dependencies import get_current_user
from app.crud.pathology import (
    create_pathology,
    get_pathologies,
    get_pathology_by_code,
    get_pathology_by_id,
    update_pathology,
)
from app.database import get_db
from app.models.user import User
from app.schemas.pathology import (
    PathologyCreateRequest,
    PathologyResponse,
    PathologyUpdateRequest,
)


router = APIRouter(
    prefix="/api/pathologies",
    tags=["Pathologies"],
)


@router.get(
    "",
    response_model=list[PathologyResponse],
)
def read_pathologies(
    crop_name: str | None = Query(
        default=None,
        min_length=1,
    ),
    is_active: bool | None = Query(
        default=None,
    ),
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> list[PathologyResponse]:
    pathologies = get_pathologies(
        db,
        crop_name=crop_name,
        is_active=is_active,
    )

    return [
        PathologyResponse.model_validate(
            pathology
        )
        for pathology in pathologies
    ]


@router.get(
    "/code/{code}",
    response_model=PathologyResponse,
)
def read_pathology_by_code(
    code: str,
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> PathologyResponse:
    pathology = get_pathology_by_code(
        db,
        code,
    )

    if pathology is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Pathologie introuvable.",
        )

    return PathologyResponse.model_validate(
        pathology
    )


@router.get(
    "/{pathology_id}",
    response_model=PathologyResponse,
)
def read_pathology(
    pathology_id: int,
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> PathologyResponse:
    pathology = get_pathology_by_id(
        db,
        pathology_id,
    )

    if pathology is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Pathologie introuvable.",
        )

    return PathologyResponse.model_validate(
        pathology
    )


@router.post(
    "",
    response_model=PathologyResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_new_pathology(
    payload: PathologyCreateRequest,
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> PathologyResponse:
    existing = get_pathology_by_code(
        db,
        payload.code,
    )

    if existing is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=(
                "Une pathologie utilise déjà "
                "ce code."
            ),
        )

    try:
        pathology = create_pathology(
            db,
            payload,
        )

    except IntegrityError as exc:
        db.rollback()

        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=(
                "Le code de pathologie existe déjà."
            ),
        ) from exc

    return PathologyResponse.model_validate(
        pathology
    )


@router.put(
    "/{pathology_id}",
    response_model=PathologyResponse,
)
def update_existing_pathology(
    pathology_id: int,
    payload: PathologyUpdateRequest,
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> PathologyResponse:
    pathology = get_pathology_by_id(
        db,
        pathology_id,
    )

    if pathology is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Pathologie introuvable.",
        )

    if payload.code:
        existing = get_pathology_by_code(
            db,
            payload.code,
        )

        if (
            existing is not None
            and existing.id != pathology_id
        ):
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail=(
                    "Une pathologie utilise déjà "
                    "ce code."
                ),
            )

    try:
        pathology = update_pathology(
            db,
            pathology,
            payload,
        )

    except IntegrityError as exc:
        db.rollback()

        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=(
                "Le code de pathologie existe déjà."
            ),
        ) from exc

    return PathologyResponse.model_validate(
        pathology
    )


@router.patch(
    "/{pathology_id}/deactivate",
    response_model=PathologyResponse,
)
def deactivate_pathology(
    pathology_id: int,
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> PathologyResponse:
    pathology = get_pathology_by_id(
        db,
        pathology_id,
    )

    if pathology is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Pathologie introuvable.",
        )

    pathology.is_active = False

    db.commit()
    db.refresh(pathology)

    return PathologyResponse.model_validate(
        pathology
    )


@router.patch(
    "/{pathology_id}/activate",
    response_model=PathologyResponse,
)
def activate_pathology(
    pathology_id: int,
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> PathologyResponse:
    pathology = get_pathology_by_id(
        db,
        pathology_id,
    )

    if pathology is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Pathologie introuvable.",
        )

    pathology.is_active = True

    db.commit()
    db.refresh(pathology)

    return PathologyResponse.model_validate(
        pathology
    )
