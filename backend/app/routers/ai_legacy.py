from fastapi import APIRouter, Depends
from pydantic import BaseModel
from typing import List, Optional

from app.core.dependencies import get_current_user
from app.models.user import User

router = APIRouter(
    prefix="/api/ai",
    tags=["AI Legacy Compat"],
)

class Part(BaseModel):
    text: Optional[str] = None

class Content(BaseModel):
    parts: List[Part]

class GenerateRequest(BaseModel):
    contents: List[Content]
    systemInstruction: Optional[Content] = None
    generationConfig: Optional[dict] = None

@router.post("/generate")
def generate_legacy(
    payload: GenerateRequest,
    current_user: User = Depends(get_current_user),
):
    """
    Stub pour compatibilité avec les anciens APK qui appelaient POST /api/ai/generate
    directement depuis l'app. Le nouveau flux doit utiliser POST /api/scans/diagnose
    qui fait Gemini + Cloudinary + catalogue côté backend.

    On retourne une réponse factice pour éviter un crash, mais on recommande
    fortement de migrer vers /api/scans/diagnose.
    """
    # Extraire le dernier message utilisateur si possible
    last_text = ""
    if payload.contents:
        last = payload.contents[-1]
        if last.parts:
            last_text = last.parts[0].text or ""

    return {
        "candidates": [
            {
                "content": {
                    "parts": [
                        {
                            "text": f"Mode compatibilité : l'IA backend legacy est désactivée. Utilisez POST /api/scans/diagnose avec image. Requête reçue : {last_text[:200]}"
                        }
                    ]
                }
            }
        ]
    }
