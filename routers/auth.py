from fastapi import APIRouter, HTTPException, Body, Depends
from database import get_db
from models import User
from sqlalchemy.orm import Session
from fastapi.security import OAuth2PasswordBearer
import jwt
import datetime

router = APIRouter()
SECRET_KEY = "your_secret_key"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_DAYS = 60

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")


def create_access_token(data: dict, expires_delta: int = ACCESS_TOKEN_EXPIRE_DAYS):
    """Создание JWT-токена"""
    to_encode = data.copy()
    expire = datetime.datetime.utcnow() + datetime.timedelta(days=expires_delta)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)


def verify_token(token: str):
    """Проверка JWT-токена"""
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token expired")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Invalid token")


@router.post("/register")
def register(username: str = Body(...), email: str = Body(...), password: str = Body(...), db: Session = Depends(get_db)):
    if db.query(User).filter(User.username == username).first():
        raise HTTPException(status_code=400, detail="Username already exists")
    user = User(username=username, email=email)
    user.set_password(password)
    db.add(user)
    db.commit()
    db.refresh(user)
    return {"message": "User registered successfully", "user_id": user.id}


@router.post("/login")
def login(username: str = Body(...), password: str = Body(...), db: Session = Depends(get_db)):
    user = db.query(User).filter(User.username == username).first()
    if not user or not user.verify_password(password):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    token = create_access_token({"sub": user.username, "user_id": user.id})
    return {"access_token": token, "token_type": "bearer", "user_id": user.id, "username": user.username, "email": user.email}
