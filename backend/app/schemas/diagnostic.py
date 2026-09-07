from pydantic import BaseModel, Field


class DiagnosticResponse(BaseModel):
    plant_name: str
    disease_name: str
    confidence: int = Field(ge=0, le=100)
    symptoms: str
    treatment_local: str
    treatment_chemical: str
    pathology_id: int | None = None
    validation_status: str = "PENDING_REVIEW"
    catalog_version: str | None = None
