from uuid import uuid4
from urllib.parse import quote

import httpx

from app.config import settings


class SupabaseStorageError(Exception):
    """Image storage errors reported without leaking credentials."""


class SupabaseStorageService:
    def __init__(self) -> None:
        if not settings.supabase_url or not settings.supabase_service_role_key:
            raise SupabaseStorageError(
                "Supabase Storage n'est pas configuré (SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY)."
            )
        self.base_url = settings.supabase_url.rstrip("/")
        self.key = settings.supabase_service_role_key
        self.bucket = settings.supabase_storage_bucket

    def upload_diagnostic_image(
        self, *, image_bytes: bytes, filename: str | None,
        mime_type: str, user_id: int,
    ) -> str:
        suffix = "webp" if mime_type == "image/webp" else "png" if mime_type == "image/png" else "jpg"
        object_path = f"user_{user_id}/{uuid4().hex}.{suffix}"
        endpoint = (
            f"{self.base_url}/storage/v1/object/"
            f"{quote(self.bucket, safe='')}/{quote(object_path, safe='/')}"
        )
        try:
            with httpx.Client(timeout=45.0) as client:
                response = client.post(
                    endpoint,
                    headers={
                        "apikey": self.key,
                        "Authorization": f"Bearer {self.key}",
                        "Content-Type": mime_type,
                        "x-upsert": "false",
                    },
                    content=image_bytes,
                )
                response.raise_for_status()
        except httpx.HTTPError as exc:
            raise SupabaseStorageError("La photo n'a pas pu être stockée dans Supabase Storage.") from exc
        return object_path

    def create_signed_url(self, object_path: str, expires_in: int = 3600) -> str:
        endpoint = (
            f"{self.base_url}/storage/v1/object/sign/"
            f"{quote(self.bucket, safe='')}/{quote(object_path, safe='/')}"
        )
        try:
            with httpx.Client(timeout=20.0) as client:
                response = client.post(
                    endpoint,
                    headers={"apikey": self.key, "Authorization": f"Bearer {self.key}"},
                    json={"expiresIn": expires_in},
                )
                response.raise_for_status()
                signed_path = response.json().get("signedURL")
            if not isinstance(signed_path, str) or not signed_path:
                raise ValueError("Missing signed URL")
            return signed_path if signed_path.startswith("http") else f"{self.base_url}/storage/v1{signed_path if signed_path.startswith('/') else '/' + signed_path}"
        except (httpx.HTTPError, ValueError, TypeError) as exc:
            raise SupabaseStorageError("Impossible de créer le lien sécurisé de la photo.") from exc
