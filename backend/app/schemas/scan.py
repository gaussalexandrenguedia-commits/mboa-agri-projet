from pydantic import BaseModel, Field


class ScanCreateRequest(BaseModel):
    local_id: int = Field(ge=0)
    user_id: int | None = Field(default=None, ge=1)
    pathology_id: int | None = Field(default=None, ge=1)
    commune_id: int | None = Field(default=None, ge=1)
    hors_ligne: bool = False
    plant_name: str = Field(min_length=1, max_length=100)
    disease_name: str = Field(min_length=1, max_length=150)
    confidence: int = Field(ge=0, le=100)
    symptoms: str = Field(min_length=1)
    treatment_local: str = Field(min_length=1)
    treatment_chemical: str = Field(min_length=1)
    timestamp: int = Field(ge=0)
    latitude: float | None = Field(default=None, ge=-90, le=90)
    longitude: float | None = Field(default=None, ge=-180, le=180)


class ScanResponse(BaseModel):
    id: int
    local_id: int
    user_id: int | None

    pathology_id: int | None
    commune_id: int | None
    hors_ligne: bool

    plant_name: str
    disease_name: str
    confidence: int
    symptoms: str
    treatment_local: str
    treatment_chemical: str
    timestamp: int
    latitude: float | None
    longitude: float | None

    model_config = {"from_attributes": True}
