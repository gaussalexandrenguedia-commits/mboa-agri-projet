from datetime import datetime

from pydantic import BaseModel, Field


class AlertResponse(BaseModel):
    id: int
    pathology_id: int
    commune_id: int
    scan_count: int = Field(ge=0)
    alert_level: str
    created_at: datetime

    model_config = {"from_attributes": True}

class AlertCreateRequest(BaseModel):
    pathology_id: int = Field(ge=1)
    commune_id: int = Field(ge=1)
    scan_count: int = Field(ge=0)
    alert_level: str = Field(min_length=1, max_length=20)