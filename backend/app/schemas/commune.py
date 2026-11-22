from pydantic import BaseModel, Field


class CommuneResponse(BaseModel):
    id: int
    code: str
    name: str
    postal_code: str | None = None
    is_active: bool = True

    model_config = {"from_attributes": True}


class CommunePageResponse(BaseModel):
    items: list[CommuneResponse]
    total: int = Field(ge=0)
    page: int = Field(ge=1)
    page_size: int = Field(ge=1)
    pages: int = Field(ge=0)


class CommuneCreateRequest(BaseModel):
    code: str = Field(min_length=1, max_length=50)
    name: str = Field(min_length=1, max_length=100)
    postal_code: str | None = Field(default=None, max_length=20)


class CommuneUpdateRequest(BaseModel):
    code: str | None = Field(default=None, min_length=1, max_length=50)
    name: str | None = Field(default=None, min_length=1, max_length=100)
    postal_code: str | None = Field(default=None, max_length=20)
