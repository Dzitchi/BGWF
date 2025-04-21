from fastapi import APIRouter, Header, Depends, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy import func
from database import get_db
from models import Group, GroupMember, GroupInvitation, User, Game, UserGame, Genre, PlayedGame, Rating
from routers.auth import verify_token
from utils.websocket import active_connections
from datetime import datetime

router = APIRouter()


@router.post("/groups")
async def create_group(authorization: str = Header(...), db: Session = Depends(get_db)):
    """Создать новую группу."""
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split(" ")[1]
    payload = verify_token(token)
    creator_id = payload.get("user_id")

    group = Group(creator_id=creator_id)
    db.add(group)
    db.commit()
    db.refresh(group)

    # Автоматически добавляем создателя в участники
    group_member = GroupMember(group_id=group.id, user_id=creator_id)
    db.add(group_member)
    db.commit()

    return {"message": "Group created successfully", "group_id": group.id}


@router.post("/groups/{group_id}/invite/{receiver_id}")
async def invite_to_group(group_id: int, receiver_id: int, authorization: str = Header(...),
                          db: Session = Depends(get_db)):
    """Отправить приглашение в группу."""
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split(" ")[1]
    payload = verify_token(token)
    sender_id = payload.get("user_id")

    sender = db.query(User).filter(User.id == sender_id).first()
    group = db.query(Group).filter(Group.id == group_id).first()
    if not group:
        raise HTTPException(status_code=404, detail="Group not found")

    if group.creator_id != sender_id:
        raise HTTPException(status_code=403, detail="Only group creator can send invitations")

    if sender_id == receiver_id:
        raise HTTPException(status_code=400, detail="Cannot invite yourself")

    # Проверяем, не является ли пользователь уже участником
    existing_member = db.query(GroupMember).filter(GroupMember.group_id == group_id,
                                                   GroupMember.user_id == receiver_id).first()
    if existing_member:
        raise HTTPException(status_code=400, detail="User is already a group member")

    # Проверяем, не отправлено ли уже приглашение
    existing_invitation = db.query(GroupInvitation).filter(
        GroupInvitation.group_id == group_id,
        GroupInvitation.receiver_id == receiver_id,
        GroupInvitation.status == "pending"
    ).first()
    if existing_invitation:
        raise HTTPException(status_code=400, detail="Invitation already sent")

    invitation = GroupInvitation(group_id=group_id, sender_id=sender_id, receiver_id=receiver_id)
    db.add(invitation)
    db.commit()

    # Уведомляем получателя через WebSocket
    if receiver_id in active_connections:
        await active_connections[receiver_id].send_json({
            "type": "group_invitation_received",
            "group_id": group_id,
            "from_user_id": sender_id,
            "username": sender.username
        })

    return {"message": "Invitation sent successfully"}


@router.post("/groups/invitations/{invitation_id}/respond")
async def respond_to_invitation(invitation_id: int, response: str, authorization: str = Header(...),
                                db: Session = Depends(get_db)):
    """Ответить на приглашение в группу."""
    if response not in ["accepted", "rejected"]:
        raise HTTPException(status_code=400, detail="Invalid response")

    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split(" ")[1]
    payload = verify_token(token)
    user_id = payload.get("user_id")

    user = db.query(User).filter(User.id == user_id).first()
    invitation = db.query(GroupInvitation).filter(GroupInvitation.id == invitation_id).first()
    if not invitation or invitation.receiver_id != user_id:
        raise HTTPException(status_code=404, detail="Invitation not found")

    invitation.status = response
    if response == "accepted":
        # Добавляем пользователя в группу
        group_member = GroupMember(group_id=invitation.group_id, user_id=user_id)
        db.add(group_member)

    db.commit()

    # Уведомляем отправителя через WebSocket
    if invitation.sender_id in active_connections:
        await active_connections[invitation.sender_id].send_json({
            "type": "group_invitation_response",
            "response": response,
            "from_user_id": user_id,
            "username": user.username,
            "group_id": invitation.group_id
        })

    return {"message": f"Invitation {response}"}


@router.get("/groups/invitations")
def get_incoming_invitations(authorization: str = Header(...), db: Session = Depends(get_db)):
    """Получить список входящих приглашений в группы."""
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split(" ")[1]
    payload = verify_token(token)
    user_id = payload.get("user_id")

    invitations = (
        db.query(GroupInvitation, User.username)
        .join(User, User.id == GroupInvitation.sender_id)
        .filter(GroupInvitation.receiver_id == user_id, GroupInvitation.status == "pending")
        .all()
    )

    return [
        {
            "id": inv.GroupInvitation.id,
            "group_id": inv.GroupInvitation.group_id,
            "sender_id": inv.GroupInvitation.sender_id,
            "sender_name": inv.username
        }
        for inv in invitations
    ]


@router.get("/groups/{group_id}/members")
def get_group_members(group_id: int, authorization: str = Header(...), db: Session = Depends(get_db)):
    """Получить всех участников группы."""
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split(" ")[1]
    payload = verify_token(token)
    user_id = payload.get("user_id")

    # Проверяем существование группы
    group = db.query(Group).filter(Group.id == group_id).first()
    if not group:
        raise HTTPException(status_code=404, detail="Group not found")

    # Проверяем, является ли пользователь участником группы
    is_member = db.query(GroupMember).filter(GroupMember.group_id == group_id, GroupMember.user_id == user_id).first()
    if not is_member:
        raise HTTPException(status_code=403, detail="You are not a member of this group")

    # Получаем всех участников группы
    members = db.query(User).join(GroupMember).filter(GroupMember.group_id == group_id).all()
    return [{"id": member.id, "username": member.username} for member in members]


@router.get("/groups/{group_id}/games")
def get_group_games(
    group_id: int,
    genres: str = None,
    min_players: int = None,
    max_players: int = None,
    min_play_time: int = None,
    max_play_time: int = None,
    authorization: str = Header(...),
    db: Session = Depends(get_db)
):
    """Получить уникальный список игр всех участников группы с фильтрами."""
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split(" ")[1]
    payload = verify_token(token)
    user_id = payload.get("user_id")

    # Проверяем существование группы
    group = db.query(Group).filter(Group.id == group_id).first()
    if not group:
        raise HTTPException(status_code=404, detail="Group not found")

    # Проверяем, является ли пользователь участником группы
    is_member = db.query(GroupMember).filter(GroupMember.group_id == group_id, GroupMember.user_id == user_id).first()
    if not is_member:
        raise HTTPException(status_code=403, detail="You are not a member of this group")

    # Получаем ID всех участников группы
    member_ids = [m.user_id for m in db.query(GroupMember).filter(GroupMember.group_id == group_id).all()]

    games_query = db.query(Game).join(UserGame).filter(UserGame.user_id.in_(member_ids)).distinct().join(Game.genre,
                                                                                                         isouter=True)

    # Применяем фильтры
    if genres:
        genre_list = [g.strip() for g in genres.split(",")]
        games_query = games_query.filter(Game.genre.has(Genre.name.in_(genre_list)))

    if min_players is not None:
        games_query = games_query.filter(Game.min_players <= min_players)
    if max_players is not None:
        games_query = games_query.filter(Game.max_players >= max_players)

    if min_play_time is not None:
        games_query = games_query.filter(Game.play_time >= min_play_time)
    if max_play_time is not None:
        games_query = games_query.filter(Game.play_time <= max_play_time)

    games = games_query.all()

    # считаем во сколько игр сыграл каждый участник
    user_played_counts = db.query(User.id, func.count(PlayedGame.game_id)).join(PlayedGame).filter(
        User.id.in_(member_ids)).group_by(User.id).all()
    user_played_dict = {user_id: count for user_id, count in user_played_counts}

    game_data = []
    for game in games:
        # Оценки от пользователей группы
        ratings = db.query(Rating).filter(Rating.game_id == game.id, Rating.user_id.in_(member_ids)).all()
        if ratings:
            # Вычисляем взвешенную среднюю оценку
            weighted_sum = sum(r.rating * user_played_dict.get(r.user_id, 0) for r in ratings)
            total_weight = sum(user_played_dict.get(r.user_id, 0) for r in ratings)
            weighted_avg = weighted_sum / total_weight if total_weight > 0 else 0
            # Находим количество низких оценок
            low_rating_count = sum(1 for r in ratings if r.rating < 3) + sum(1 for r in ratings if r.rating < 2)
        else:
            weighted_avg = 5  # Игры без оценок имеют низкий приоритет
            low_rating_count = 0

        # Время последней игры
        last_played = db.query(func.max(PlayedGame.last_played)).filter(
            PlayedGame.game_id == game.id,
            PlayedGame.user_id.in_(member_ids)
        ).scalar()
        if last_played is None:
            last_played = datetime.min  # Неигранные игры считаем давно не игранными

        game_data.append({
            "game": game,
            "weighted_avg": weighted_avg,
            "last_played": last_played,
            "low_rating_count": low_rating_count
        })

        print(game_data)
        print("-----------------------")

    # Сортируем игры по трём критериям
    sorted_games = sorted(
        game_data,
        key=lambda x: (-x["weighted_avg"], x["last_played"], x["low_rating_count"])
    )

    # Формируем результат
    return [{
        "id": gd["game"].id,
        "title": gd["game"].title,
        "min_players": gd["game"].min_players,
        "max_players": gd["game"].max_players,
        "play_time": gd["game"].play_time,
        "genre": gd["game"].genre.name if gd["game"].genre else None,
        "description": gd["game"].description,
        "image_url": gd["game"].image_url
    } for gd in sorted_games]


@router.get("/groups/my")
def get_user_groups(
    authorization: str = Header(...),
    db: Session = Depends(get_db)
):
    """
    Получить список всех групп, в которых состоит текущий пользователь.
    Возвращает список объектов групп с основной информацией.
    """
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split(" ")[1]
    payload = verify_token(token)
    user_id = payload.get("user_id")

    # Получаем все группы пользователя с информацией о создателе
    groups = (
        db.query(
            Group.id,
            Group.created_at,
            Group.creator_id,                           # <-- добавили здесь
            User.username.label("creator_name")
        )
        .join(User, Group.creator_id == User.id)
        .join(GroupMember, Group.id == GroupMember.group_id)
        .filter(GroupMember.user_id == user_id)
        .all()
    )

    return [{
        "group_id":    group.id,
        "created_at":  group.created_at,
        "creator_id":  group.creator_id,               # теперь существует
        "username":    group.creator_name
    } for group in groups]


@router.post("/groups/{group_id}/play/{game_id}")
def play_game_for_group(
    group_id: int,
    game_id: int,
    authorization: str = Header(...),
    db: Session = Depends(get_db)
):
    # Проверяем JWT
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")
    token = authorization.split(" ", 1)[1]
    payload = verify_token(token)
    user_id = payload.get("user_id")

    # Проверяем, что пользователь в группе
    is_member = db.query(GroupMember).filter(
        GroupMember.group_id == group_id,
        GroupMember.user_id == user_id
    ).first()
    if not is_member:
        raise HTTPException(status_code=403, detail="You are not a member of this group")

    # Берём всех участников
    member_ids = [m.user_id for m in db.query(GroupMember)
                  .filter(GroupMember.group_id == group_id).all()]

    # Обновляем или создаём PlayedGame для каждого
    for uid in member_ids:
        pg = db.query(PlayedGame).filter(
            PlayedGame.user_id == uid,
            PlayedGame.game_id == game_id
        ).first()
        if pg:
            pg.last_played = datetime.utcnow()
        else:
            db.add(PlayedGame(user_id=uid, game_id=game_id, last_played=datetime.utcnow()))
    db.commit()
    return {"message": "Marked played for all group members"}
