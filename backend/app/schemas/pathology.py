from pydantic import BaseModel, Field


class PathologyBase(BaseModel):
    code: str = Field(
        min_length=1,
        max_length=80,
    )

    technical_name: str | None = Field(
        default=None,
        max_length=100,
    )

    common_name: str = Field(
        min_length=1,
        max_length=100,
    )

    crop_name: str = Field(
        min_length=1,
        max_length=50,
    )

    key_symptoms: str | None = None

    biological_treatment: str | None = None

    chemical_treatment: str | None = None

    default_severity: str = Field(
        default="Attention",
        max_length=20,
    )


class PathologyCreateRequest(PathologyBase):
    is_active: bool = True


class PathologyUpdateRequest(BaseModel):
    code: str | None = Field(
        default=None,
        min_length=1,
        max_length=80,
    )

    technical_name: str | None = Field(
        default=None,
        max_length=100,
    )

    common_name: str | None = Field(
        default=None,
        min_length=1,
        max_length=100,
    )

    crop_name: str | None = Field(
        default=None,
        min_length=1,
        max_length=50,
    )

    key_symptoms: str | None = None

    biological_treatment: str | None = None

    chemical_treatment: str | None = None

    default_severity: str | None = Field(
        default=None,
        max_length=20,
    )

    is_active: bool | None = None


class PathologyResponse(PathologyBase):
    id: int
    is_active: bool

    model_config = {
        "from_attributes": True,
    }
