from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import model_validator


class Settings(BaseSettings):
    app_name: str = "MBOA AGRI API"
    environment: str = "development"
    database_url: str

    jwt_secret_key: str = "secret-key"
    cors_origins: list[str] = []
    login_rate_limit: int = 10
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 60

    gemini_api_key: str | None = None
    gemini_model: str = "gemini-2.0-flash"

    cloudinary_cloud_name: str | None = None
    cloudinary_api_key: str | None = None
    cloudinary_api_secret: str | None = None
    cloudinary_folder: str = "mboa_agri/diagnostics"

    @model_validator(mode="after")
    def validate_production_secrets(self):
        if self.environment.lower() in {"production", "prod"} and (
            self.jwt_secret_key == "secret-key" or len(self.jwt_secret_key) < 32
        ):
            raise ValueError("JWT_SECRET_KEY must be configured with at least 32 characters in production.")
        return self

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )


settings = Settings()
