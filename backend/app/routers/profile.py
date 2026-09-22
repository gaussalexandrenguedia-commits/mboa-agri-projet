from fastapi import APIRouter, Depends
from pydantic import BaseModel

from app.core.dependencies import get_current_user
from app.models.user import User

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
):
    """
    Endpoint de compatibilité pour les anciennes versions Android
    qui poussaient le profil vers /api/profiles.
    Le nouveau flux utilise commune_code dans /api/scans et /api/scans/diagnose.
    On retourne simplement un succès pour ne pas casser les anciens APK.
    """
    return {
        "status": "ok",
        "user_id": current_user.id,
        "username": payload.username,
        "commune": payload.commune,
        "message": "Profil reçu (compatibilité legacy)",
    }
