from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.user import User

from app.core.security import hash_password


def get_user_by_phone(db: Session, phone_number: str) -> User | None:
    statement = select(User).where(User.phone_number == phone_number)
    return db.scalar(statement)


def create_user(
    db: Session,
    username: str,
    phone_number: str,
    password_hash: str,
) -> User:
    user = User(
        username=username,
        phone_number=phone_number,
        password_hash=password_hash,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


def update_user(
    db: Session,
    user: User,
    username: str | None = None,
    phone_number: str | None = None,
) -> User:
    if username is not None:
        user.username = username

    if phone_number is not None:
        user.phone_number = phone_number

    db.commit()
    db.refresh(user)
    return user


def update_user_password(
    db: Session,
    user: User,
    new_password: str,
) -> User:
    user.password_hash = hash_password(new_password)
    db.commit()
    db.refresh(user)
    return user
