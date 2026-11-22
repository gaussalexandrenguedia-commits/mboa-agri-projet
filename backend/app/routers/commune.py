from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.dependencies import get_current_user
from app.database import get_db
from app.models.commune import Commune
from app.models.user import User
from app.schemas.commune import (
    CommuneCreateRequest,
    CommunePageResponse,
    CommuneResponse,
    CommuneUpdateRequest,
)


router = APIRouter(
    prefix="/api/communes",
    tags=["Communes"],
)


def _get_or_404(db: Session, commune_id: int) -> Commune:
    commune = db.get(Commune, commune_id)
    if commune is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Commune introuvable.",
        )
    return commune


@router.get("", response_model=CommunePageResponse)
def read_communes(
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    is_active: bool | None = Query(default=True),
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> CommunePageResponse:
    """Liste paginée des communes (source des `commune_code` côté mobile)."""
    filters = []
    if is_active is not None and hasattr(Commune, "is_active"):
        filters.append(Commune.is_active.is_(is_active))

    total = db.scalar(
        select(func.count()).select_from(Commune).where(*filters)
    ) or 0

    communes = list(
        db.scalars(
            select(Commune)
            .where(*filters)
            .order_by(Commune.name.asc())
            .offset((page - 1) * page_size)
            .limit(page_size)
        ).all()
    )

    pages = (total + page_size - 1) // page_size if total else 0

    return CommunePageResponse(
        items=[CommuneResponse.model_validate(commune) for commune in communes],
        total=total,
        page=page,
        page_size=page_size,
        pages=pages,
    )


@router.post("", response_model=CommuneResponse, status_code=status.HTTP_201_CREATED)
def create_new_commune(
    payload: CommuneCreateRequest,
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> CommuneResponse:
    existing = db.scalar(
        select(Commune).where(
            (Commune.code == payload.code.strip())
            | (Commune.name == payload.name.strip())
        )
    )
    if existing is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Code ou nom déjà utilisé.",
        )

    commune = Commune(
        code=payload.code.strip(),
        name=payload.name.strip(),
        postal_code=(payload.postal_code or "").strip() or None,
    )
    db.add(commune)
    db.commit()
    db.refresh(commune)

    return CommuneResponse.model_validate(commune)


@router.put("/{commune_id}", response_model=CommuneResponse)
def update_existing_commune(
    commune_id: int,
    payload: CommuneUpdateRequest,
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> CommuneResponse:
    commune = _get_or_404(db, commune_id)

    if payload.code is not None:
        commune.code = payload.code.strip()
    if payload.name is not None:
        commune.name = payload.name.strip()
    if payload.postal_code is not None:
        commune.postal_code = payload.postal_code.strip()

    db.add(commune)
    db.commit()
    db.refresh(commune)

    return CommuneResponse.model_validate(commune)


@router.patch("/{commune_id}/deactivate", response_model=CommuneResponse)
def deactivate_commune(
    commune_id: int,
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> CommuneResponse:
    commune = _get_or_404(db, commune_id)

    if not hasattr(Commune, "is_active"):
        raise HTTPException(
            status_code=status.HTTP_501_NOT_IMPLEMENTED,
            detail="Colonne is_active absente : appliquez la migration des communes.",
        )

    commune.is_active = False
    db.add(commune)
    db.commit()
    db.refresh(commune)

    return CommuneResponse.model_validate(commune)


@router.patch("/{commune_id}/activate", response_model=CommuneResponse)
def activate_commune(
    commune_id: int,
    db: Session = Depends(get_db),
    _: User = Depends(get_current_user),
) -> CommuneResponse:
    commune = _get_or_404(db, commune_id)

    if not hasattr(Commune, "is_active"):
        raise HTTPException(
            status_code=status.HTTP_501_NOT_IMPLEMENTED,
            detail="Colonne is_active absente : appliquez la migration des communes.",
        )

    commune.is_active = True
    db.add(commune)
    db.commit()
    db.refresh(commune)

    return CommuneResponse.model_validate(commune)
