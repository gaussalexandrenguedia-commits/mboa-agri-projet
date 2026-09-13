from typing import Any
from uuid import uuid4

import httpx

from app.config import settings


class CloudinaryServiceError(Exception):
    """
    Erreur contrôlée lors de l'envoi d'une image à Cloudinary.
    """


class CloudinaryService:
    def __init__(self) -> None:
        missing = [
            name
            for name, value in (
                (
                    "CLOUDINARY_CLOUD_NAME",
                    settings.cloudinary_cloud_name,
                ),
                (
                    "CLOUDINARY_API_KEY",
                    settings.cloudinary_api_key,
                ),
                (
                    "CLOUDINARY_API_SECRET",
                    settings.cloudinary_api_secret,
                ),
            )
            if not value
        ]

        if missing:
            raise CloudinaryServiceError(
                "Configuration Cloudinary incomplète : "
                + ", ".join(missing)
            )

        cloud_name = settings.cloudinary_cloud_name
        api_key = settings.cloudinary_api_key
        api_secret = settings.cloudinary_api_secret

        if not isinstance(cloud_name, str):
            raise CloudinaryServiceError(
                "CLOUDINARY_CLOUD_NAME doit être une chaîne."
            )

        if not isinstance(api_key, str):
            raise CloudinaryServiceError(
                "CLOUDINARY_API_KEY doit être une chaîne."
            )

        if not isinstance(api_secret, str):
            raise CloudinaryServiceError(
                "CLOUDINARY_API_SECRET doit être une chaîne."
            )

        self.cloud_name = cloud_name
        self.api_key = api_key
        self.api_secret = api_secret
        self.folder = settings.cloudinary_folder.strip("/")

    def upload_diagnostic_image(
        self,
        *,
        image_bytes: bytes,
        filename: str | None,
        mime_type: str,
        user_id: int,
    ) -> str:
        public_id = (
            f"{self.folder}/"
            f"user_{user_id}/"
            f"{uuid4().hex}"
        )

        endpoint = (
            "https://api.cloudinary.com/v1_1/"
            f"{self.cloud_name}/image/upload"
        )

        files = {
            "file": (
                filename or "diagnostic-image",
                image_bytes,
                mime_type,
            ),
        }

        data = {
            "public_id": public_id,
            "use_filename": "false",
        }

        try:
            with httpx.Client(timeout=60.0) as client:
                response = client.post(
                    endpoint,
                    data=data,
                    files=files,
                    auth=(
                        self.api_key,
                        self.api_secret,
                    ),
                )

                response.raise_for_status()

                payload: dict[str, Any] = (
                    response.json()
                )

        except (
            httpx.HTTPError,
            ValueError,
            TypeError,
        ) as exc:
            raise CloudinaryServiceError(
                "L'image n'a pas pu être stockée "
                "sur Cloudinary."
            ) from exc

        secure_url = payload.get(
            "secure_url"
        )

        if not isinstance(secure_url, str):
            raise CloudinaryServiceError(
                "Cloudinary n'a pas retourné "
                "de secure_url."
            )

        if not secure_url:
            raise CloudinaryServiceError(
                "Cloudinary a retourné une URL vide."
            )

        return secure_url


def get_cloudinary_service() -> CloudinaryService:
    return CloudinaryService()
