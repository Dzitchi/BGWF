from fastapi import APIRouter, Depends, HTTPException, Body, Header
from database import get_db
from models import Game, UserGame, Rating, User, Genre
from sqlalchemy.orm import Session, joinedload
from rapidfuzz import fuzz
from routers.auth import verify_token
from utils.utils import safe_translit

router = APIRouter()


@router.get("/games/search")
def search_games(
    query: str = "",
    genres: str = None,
    min_players: int = None,
    max_players: int = None,
    min_play_time: int = None,
    max_play_time: int = None,
    db: Session = Depends(get_db)
):
    """Поиск игр по названию и фильтрам."""
    games_query = db.query(Game).join(Game.genre, isouter=True)

    # Фильтр по жанрам
    if genres:
        genre_list = [g.strip() for g in genres.split(",")]
        games_query = games_query.filter(Game.genre.has(Genre.name.in_(genre_list)))

    # Фильтр по количеству игроков
    if min_players is not None:
        games_query = games_query.filter(Game.min_players <= min_players)
    if max_players is not None:
        games_query = games_query.filter(Game.max_players >= max_players)

    # Фильтр по продолжительности
    if min_play_time is not None:
        games_query = games_query.filter(Game.play_time >= min_play_time)
    if max_play_time is not None:
        games_query = games_query.filter(Game.play_time <= max_play_time)

    games = games_query.all()

    if not query:
        return [{
            **game.__dict__,
            "genre": game.genre.name if game.genre else None
        } for game in games]

    # Транслитерация для поиска и список с совпадениями
    translit_query = safe_translit(query)
    results = []

    for game in games:
        similarity = max(
            fuzz.ratio(query.lower(), game.title.lower()),
            fuzz.ratio(translit_query.lower(), game.title.lower())
        )
        if similarity > 40:
            results.append((game, similarity))

    # Сортируем по степени совпадения
    results.sort(key=lambda x: x[1], reverse=True)

    return [{
        **game.__dict__,
        "genre": game.genre.name if game.genre else None
    } for game, _ in results]


@router.get("/users/{user_id}/games")
def get_user_games(user_id: int, db: Session = Depends(get_db)):
    """
    Получить список игр, принадлежащих пользователю.

    Принимает ID пользователя и возвращает список игр, добавленных им в коллекцию.
    """
    games = (
        db.query(Game)
        .join(UserGame)
        .filter(UserGame.user_id == user_id)
        .options(joinedload(Game.genre))  # Жадно загружаем связанный жанр
        .all()
    )

    return [{
        "id": game.id,
        "title": game.title,
        "min_players": game.min_players,
        "max_players": game.max_players,
        "play_time": game.play_time,
        "genre": game.genre.name if game.genre else None,
        "description": game.description,
        "image_url": game.image_url,
        "created_at": game.created_at
    } for game in games]


@router.get("/games/{game_id}/ratings")
def get_game_ratings(game_id: int, db: Session = Depends(get_db)):
    """Получить список оценок игры."""
    ratings = db.query(Rating).filter(Rating.game_id == game_id).all()

    if not ratings:
        return []  # Если нет оценок, возвращаем пустой список

    return [{"user_id": r.user_id, "rating": r.rating, "review": r.review} for r in ratings]


@router.post("/games/{game_id}/rate")
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


@router.get("/games/{game_id}/comments")
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


@router.get("/genres")
def get_genres(db: Session = Depends(get_db)):
    genres = db.query(Genre).all()
    return [g.name for g in genres]
