from fastapi import FastAPI, HTTPException, Depends, Body, Header, APIRouter # роутер нужен на будущее, чтобы разбить код на файлы
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy import create_engine, Column, Integer, String, ForeignKey, Text, DateTime, func
from sqlalchemy.orm import declarative_base, sessionmaker, Session
from rapidfuzz import fuzz
from transliterate import translit
from passlib.context import CryptContext
import jwt
import datetime
import os

DATABASE_URL = "sqlite:///./games.db"
engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

SECRET_KEY = "your_secret_key"  # Заменить на безопасный ключ
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_DAYS = 60

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")


# Определяем модели
class User(Base):
    """Модель пользователя."""
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, nullable=False)
    email = Column(String, unique=True, nullable=False)
    password_hash = Column(String, nullable=False)
    created_at = Column(DateTime, default=func.now())

    def set_password(self, password: str):
        """Устанавливаем хеш пароля"""
        self.password_hash = pwd_context.hash(password)

    def verify_password(self, password: str) -> bool:
        """Проверяем пароль"""
        return pwd_context.verify(password, self.password_hash)


class Game(Base):
    """Модель игры."""
    __tablename__ = "games"
    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, unique=True, nullable=False)
    min_players = Column(Integer, nullable=False)
    max_players = Column(Integer, nullable=False)
    play_time = Column(Integer, nullable=True)
    genre = Column(String, nullable=True)
    description = Column(Text, nullable=True)
    image_url = Column(String, nullable=True)
    created_at = Column(DateTime, default=func.now())


class UserGame(Base):
    """Связь пользователей и игр."""
    __tablename__ = "user_games"
    user_id = Column(Integer, ForeignKey("users.id"), primary_key=True)
    game_id = Column(Integer, ForeignKey("games.id"), primary_key=True)


class Rating(Base):
    """Модель рейтинга и отзывов."""
    __tablename__ = "ratings"
    user_id = Column(Integer, ForeignKey("users.id"), primary_key=True)
    game_id = Column(Integer, ForeignKey("games.id"), primary_key=True)
    rating = Column(Integer, nullable=False)
    review = Column(Text, nullable=True)
    rated_at = Column(DateTime, default=func.now())


Base.metadata.create_all(bind=engine)

app = FastAPI()

# Папка с изображениями
IMAGE_FOLDER = "images"


def get_db():
    """Создает сессию базы данных."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def safe_translit(text):
    """Транслитерирует текст, если возможно."""
    if len(text) <= 1:  # Проверяем, чтобы запрос был не слишком коротким
        return text
    try:
        if any("а" <= c <= "я" for c in text.lower()):
            return translit(text, reversed=True)
        else:
            return translit(text, "ru")
    except Exception:
        return text  # Если не получилось транслитерировать, возвращаем оригинал


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


@app.get("/games/search")
def search_games(query: str = "", db: Session = Depends(get_db)):
    """Поиск игр по названию с учетом транслитерации."""
    games = db.query(Game).all()

    if not query:
        return games  # Если запрос пустой, возвращаем все игры

    # Транслитерация для поиска и список с совпадениями
    translit_query = safe_translit(query)
    results = []

    for game in games:
        similarity = max(
            fuzz.ratio(query.lower(), game.title.lower()),  # Обычный поиск
            fuzz.ratio(translit_query.lower(), game.title.lower())  # Сравнение с транслитерированным текстом
        )
        if similarity > 40:  # Если схожесть больше 50%, добавляем в результат
            results.append((game, similarity))

    # Сортируем по степени совпадения
    results.sort(key=lambda x: x[1], reverse=True)

    return [game for game, _ in results]


@app.get("/users/{user_id}/games")
def get_user_games(user_id: int, db: Session = Depends(get_db)):
    games = db.query(Game).join(UserGame).filter(UserGame.user_id == user_id).all()
    return games


# Обработчик для получения изображений по имени файла
@app.get("/images/{image_name}")
async def get_image(image_name: str):
    image_path = os.path.join(IMAGE_FOLDER, image_name)

    # Проверяем, существует ли файл
    if os.path.exists(image_path):
        return FileResponse(image_path)
    else:
        return {"error": "Image not found"}


# Монтируем папку с изображениями как статический ресурс
app.mount("/static", StaticFiles(directory=IMAGE_FOLDER), name="static")


@app.post("/register")
def register(username: str = Body(...), email: str = Body(...), password: str = Body(...),
             db: Session = Depends(get_db)):
    """Регистрация нового пользователя."""
    if db.query(User).filter(User.username == username).first():
        raise HTTPException(status_code=400, detail="Username already exists")

    user = User(username=username, email=email)
    user.set_password(password)

    db.add(user)
    db.commit()
    db.refresh(user)
    return {"message": "User registered successfully", "user_id": user.id}


@app.post("/login")
def login(username: str = Body(...), password: str = Body(...), db: Session = Depends(get_db)):
    """Аутентификация пользователя."""
    user = db.query(User).filter(User.username == username).first()
    if not user or not user.verify_password(password):
        raise HTTPException(status_code=401, detail="Invalid credentials")

    token = create_access_token({"sub": user.username, "user_id": user.id})
    return {"access_token": token, "token_type": "bearer", "user_id": user.id, "username": user.username,
            "email": user.email}


@app.get("/users/me")
def get_current_user(authorization: str = Header(...), db: Session = Depends(get_db)):
    """
    Получить информацию о текущем пользователе.

    Требует заголовок Authorization с JWT-токеном в формате "Bearer <token>".

    Возвращает ID, имя пользователя и email.
    """
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split(" ")[1]  # Убираем "Bearer"
    payload = verify_token(token)

    user_id = payload.get("user_id")
    user = db.query(User).filter(User.id == user_id).first()

    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    return {"id": user.id, "username": user.username, "email": user.email}


@app.get("/games/{game_id}/ratings")
def get_game_ratings(game_id: int, db: Session = Depends(get_db)):
    """Получить список оценок игры."""
    ratings = db.query(Rating).filter(Rating.game_id == game_id).all()

    if not ratings:
        return []  # Если нет оценок, возвращаем пустой список

    return [{"user_id": r.user_id, "rating": r.rating, "review": r.review} for r in ratings]


@app.post("/games/{game_id}/rate")
def rate_game(game_id: int, rating: int = Body(...), review: str = Body(""), authorization: str = Header(...),
              db: Session = Depends(get_db)):
    """
    Добавить или обновить оценку игры.

    Требует авторизацию через JWT. Оценка должна быть от 1 до 5.
    Если пользователь уже оценивал игру, его оценка обновится.
    """
    if not (1 <= rating <= 5):
        raise HTTPException(status_code=400, detail="Rating must be between 1 and 5")

    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split(" ")[1]
    payload = verify_token(token)
    user_id = payload.get("user_id")

    existing_rating = db.query(Rating).filter(Rating.user_id == user_id, Rating.game_id == game_id).first()

    if existing_rating:
        existing_rating.rating = rating
        existing_rating.review = review
    else:
        new_rating = Rating(user_id=user_id, game_id=game_id, rating=rating, review=review)
        db.add(new_rating)

    db.commit()
    return {"message": "Rating added/updated successfully"}


@app.post("/users/games/{game_id}")
def add_game_to_user(game_id: int, authorization: str = Header(...), db: Session = Depends(get_db)):
    """
    Добавить игру в коллекцию пользователя.

    Требует авторизацию через JWT. Если игра уже добавлена, вернет ошибку.
    """
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split(" ")[1]
    payload = verify_token(token)
    user_id = payload.get("user_id")

    existing_entry = db.query(UserGame).filter(UserGame.user_id == user_id, UserGame.game_id == game_id).first()
    if existing_entry:
        raise HTTPException(status_code=400, detail="Game already added")

    user_game = UserGame(user_id=user_id, game_id=game_id)
    db.add(user_game)
    db.commit()
    return {"message": "Game added successfully"}


@app.delete("/users/games/{game_id}")
def remove_game_from_user(game_id: int, authorization: str = Header(...), db: Session = Depends(get_db)):
    """
    Удалить игру из коллекции пользователя.

    Требует авторизацию через JWT. Если игры нет в коллекции, вернет ошибку.
    """
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split(" ")[1]
    payload = verify_token(token)
    user_id = payload.get("user_id")

    user_game = db.query(UserGame).filter(UserGame.user_id == user_id, UserGame.game_id == game_id).first()
    if not user_game:
        raise HTTPException(status_code=404, detail="Game not found in user's collection")

    db.delete(user_game)
    db.commit()
    return {"message": "Game removed successfully"}


@app.get("/games/{game_id}/comments")
def get_game_comments(game_id: int, db: Session = Depends(get_db)):
    """Получить комментарии пользователей к игре."""
    comments = (
        db.query(Rating, User.username)
        .join(User, Rating.user_id == User.id)
        .filter(Rating.game_id == game_id)
        .all()
    )

    if not comments:
        return []

    return [
        {"user_id": rating.user_id, "username": username, "rating": rating.rating, "review": rating.review}
        for rating, username in comments
    ]
