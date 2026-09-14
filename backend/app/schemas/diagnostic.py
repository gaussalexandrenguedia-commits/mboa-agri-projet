from pydantic import BaseModel, Field


class DiagnosticResponse(BaseModel):
    """
    Réponse commune aux diagnostics offline et online.
    """

    id: int | None = None
    local_id: int | None = None

    plant_name: str
    disease_name: str

    confidence: int = Field(
        ge=0,
        le=100,
    )

    symptoms: str | None = None
    treatment_local: str | None = None
    treatment_chemical: str | None = None

    pathology_id: int | None = None
    pathology_code: str | None = None

    commune_id: int | None = None
    commune_code: str | None = None

    latitude: float | None = Field(
        default=None,
        ge=-90,
        le=90,
    )

    longitude: float | None = Field(
        default=None,
        ge=-180,
        le=180,
    )

    image_url: str | None = None

    hors_ligne: bool = False

    severity_detected: str | None = None
    severity_default: str | None = None

    information_source: str = "CATALOG"
    validation_status: str = "PENDING_REVIEW"
    catalog_version: str | None = None
