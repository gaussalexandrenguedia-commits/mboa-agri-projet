import base64
import json
import re
from typing import Any

import httpx
from pydantic import BaseModel, Field

from app.config import settings


class GeminiPrediction(BaseModel ):
    pathology_code: str | None = None

    disease_name: str = Field(
        min_length=1,
        max_length=150,
    )

    confidence: int = Field(
        ge=0,
        le=100,
    )

    severity_detected: str = Field(
        min_length=1,
        max_length=20,
    )

    symptoms_ai: str | None = None
    treatment_local_ai: str | None = None
    treatment_chemical_ai: str | None = None


class GeminiServiceError(Exception):
    pass


class GeminiService:
    def __init__(self) -> None:
        if not settings.gemini_api_key:
            raise GeminiServiceError(
                "GEMINI_API_KEY n'est pas configurée."
            )

        self.api_key = settings.gemini_api_key
        self.model = settings.gemini_model

        self.endpoint = (
            "https://generativelanguage.googleapis.com/v1beta/"
            f"models/{self.model}:generateContent"
         )

    def diagnose(
        self,
        *,
        image_bytes: bytes,
        mime_type: str,
        plant_name: str,
        symptoms: str,
        pathology_catalog: list[dict[str, str]],
    ) -> GeminiPrediction:
        prompt = f"""
Tu identifies les maladies des plantes à partir d'une image.

Culture :
{plant_name}

Symptômes fournis :
{symptoms or "Aucun symptôme fourni."}

Catalogue officiel autorisé :
{json.dumps(pathology_catalog, ensure_ascii=False)}

Retourne uniquement un JSON valide avec exactement ces champs :

{{
  "pathology_code": "CODE_EXISTANT ou null",
  "disease_name": "Nom de la maladie",
  "confidence": 0,
  "severity_detected": "Attention|Alerte|Urgence",
  "symptoms_ai": "Symptômes ou null",
  "treatment_local_ai": "Traitement local provisoire ou null",
  "treatment_chemical_ai": "Traitement chimique provisoire ou null"
}}

Règles obligatoires :

1. Si une maladie du catalogue correspond, utilise exactement son code.
2. Si aucune maladie ne correspond, pathology_code doit être null.
3. Ne crée jamais de faux code officiel.
4. La confiance doit être un nombre entre 0 et 100.
5. La gravité doit être Attention, Alerte ou Urgence.
6. Les traitements générés par l'IA sont provisoires.
7. N'ajoute aucun texte avant ou après le JSON.
"""

        request_body = {
            "contents": [
                {
                    "parts": [
                        {
                            "text": prompt,
                        },
                        {
                            "inline_data": {
                                "mime_type": mime_type,
                                "data": base64.b64encode(
                                    image_bytes
                                ).decode("ascii"),
                            },
                        },
                    ],
                }
            ],
            "generationConfig": {
                "temperature": 0.1,
                "responseMimeType": "application/json",
            },
        }

        try:
            with httpx.Client(timeout=90.0 ) as client:
                response = client.post(
                    self.endpoint,
                    params={
                        "key": self.api_key,
                    },
                    json=request_body,
                )

                response.raise_for_status()

            response_data = response.json()
            raw_text = self._extract_text(response_data)
            json_text = self._clean_json(raw_text)

            return GeminiPrediction.model_validate(
                json.loads(json_text)
            )

        except (
            httpx.HTTPError,
            ValueError,
            KeyError,
            TypeError,
         ) as exc:
            raise GeminiServiceError(
                "La réponse Gemini est indisponible ou invalide."
            ) from exc

    @staticmethod
    def _extract_text(
        response_data: dict[str, Any],
    ) -> str:
        candidates = response_data.get(
            "candidates",
            [],
        )

        for candidate in candidates:
            content = candidate.get(
                "content",
                {},
            )

            parts = content.get(
                "parts",
                [],
            )

            for part in parts:
                text = part.get("text")

                if text:
                    return text

        raise ValueError(
            "Réponse Gemini sans texte."
        )

    @staticmethod
    def _clean_json(value: str) -> str:
        value = value.strip()

        value = re.sub(
            r"^```json\s*",
            "",
            value,
            flags=re.IGNORECASE,
        )

        value = re.sub(
            r"^```\s*",
            "",
            value,
        )

        value = re.sub(
            r"\s*```$",
            "",
            value,
        )

        return value.strip()
