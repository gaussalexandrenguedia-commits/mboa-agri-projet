from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.security import hash_password, create_access_token, verify_password
from app.crud.user import create_user, get_user_by_phone
from app.database import get_db
from app.schemas.auth import RegisterRequest, UserResponse
from app.models.user import User

from app.schemas.auth import LoginRequest, TokenResponse


router = APIRouter(
    prefix="/auth",
    tags=["Authentication"],
)


@router.post(
    "/register",
    response_model=UserResponse,
    status_code=status.HTTP_201_CREATED,
)
def register(
    data: RegisterRequest,
    db: Session = Depends(get_db),
) -> User:
    phone_number = data.phone_number.strip()

    existing_user = get_user_by_phone(db, phone_number)
    if existing_user is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Ce numéro de téléphone est déjà utilisé.",
        )

    user = create_user(
        db=db,
        username=data.username.strip(),
        phone_number=phone_number,
        password_hash=hash_password(data.password),
    )

    return user


@router.post("/login", response_model=TokenResponse)
def login(
    data: LoginRequest,
    db: Session = Depends(get_db),
) -> TokenResponse:
    phone_number = data.phone_number.strip()
    user = get_user_by_phone(db, phone_number)

    if user is None or not verify_password(data.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Numéro de téléphone ou mot de passe incorrect.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Ce compte est désactivé.",
        )

    access_token = create_access_token(str(user.id))
    return TokenResponse(access_token=access_token)
