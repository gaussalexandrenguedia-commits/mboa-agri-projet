from fastapi import APIRouter, Depends
from pydantic import BaseModel

from app.core.dependencies import get_current_user
from app.models.user import User
from app.database import get_db
from sqlalchemy.orm import Session

router = APIRouter(
    prefix="/api/profiles",
    tags=["Profiles (legacy compat)"],
)

class ProfilePayload(BaseModel):
    username: str
    commune: str = ""
    cultures: str = ""
    langue: str = "fr"
    consentement_alertes: bool = False

@router.post("", status_code=201)
def upload_profile(
    payload: ProfilePayload,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    current_user.commune = payload.commune.strip()[:120]
    current_user.cultures = payload.cultures.strip()[:500]
    current_user.langue = payload.langue.strip().lower()[:10] or "fr"
    current_user.consentement_alertes = payload.consentement_alertes
    db.add(current_user)
    db.commit()
    db.refresh(current_user)
    return {"status": "ok", "user_id": current_user.id, "username": current_user.username,
            "commune": current_user.commune, "cultures": current_user.cultures,
            "langue": current_user.langue, "consentement_alertes": current_user.consentement_alertes}
