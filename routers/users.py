from fastapi import APIRouter, Header, Depends, HTTPException
from database import get_db
from sqlalchemy.orm import Session
from rapidfuzz import fuzz
from routers.auth import verify_token
from models import User, UserGame
from utils.utils import safe_translit


router = APIRouter()


@router.get("/users/me")
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


@router.post("/users/games/{game_id}")
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


@router.delete("/users/games/{game_id}")
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


@router.get("/users/search")
def search_users(query: str = "", db: Session = Depends(get_db)):
    """Поиск пользователей по имени с учетом транслитерации."""

    users = db.query(User).all()
    if not query:
        return users  # Если запрос пустой, возвращаем всех пользователей

    translit_query = safe_translit(query)
    results = []

    for user in users:
        similarity = max(
            fuzz.ratio(query.lower(), user.username.lower()),
            fuzz.ratio(translit_query.lower(), user.username.lower())
        )
        if similarity > 40:  # Если схожесть больше 40%, добавляем в результат
            results.append((user, similarity))

    results.sort(key=lambda x: x[1], reverse=True)

    return [{"id": user.id, "username": user.username, "email": user.email} for user, _ in results]
