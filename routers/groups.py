from fastapi import APIRouter, Header, Depends, HTTPException
from sqlalchemy.orm import Session
from database import get_db
from models import Group, GroupMember, GroupInvitation, User, Game, UserGame, Genre
from routers.auth import verify_token
from utils.websocket import active_connections

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
async def invite_to_group(group_id: int, receiver_id: int, authorization: str = Header(...), db: Session = Depends(get_db)):
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
    existing_member = db.query(GroupMember).filter(GroupMember.group_id == group_id, GroupMember.user_id == receiver_id).first()
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
async def respond_to_invitation(invitation_id: int, response: str, authorization: str = Header(...), db: Session = Depends(get_db)):
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

    games_query = db.query(Game).join(UserGame).filter(UserGame.user_id.in_(member_ids)).distinct().join(Game.genre, isouter=True)

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

    return [{
        "id": game.id,
        "title": game.title,
        "min_players": game.min_players,
        "max_players": game.max_players,
        "play_time": game.play_time,
        "genre": game.genre.name if game.genre else None,
        "description": game.description,
        "image_url": game.image_url
    } for game in games]


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
