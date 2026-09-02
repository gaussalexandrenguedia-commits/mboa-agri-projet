from pydantic import BaseModel, Field


class RegisterRequest(BaseModel):
    username: str = Field(min_length=2, max_length=120)
    phone_number: str = Field(min_length=8, max_length=20)
    password: str = Field(min_length=8, max_length=128)


class LoginRequest(BaseModel):
    phone_number: str = Field(min_length=8, max_length=20)
    password: str = Field(min_length=8, max_length=128)


class UserResponse(BaseModel):
    id: int
    username: str
    phone_number: str
    is_active: bool

    model_config = {"from_attributes": True}
