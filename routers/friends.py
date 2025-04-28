from functools import lru_cache
from fastapi import APIRouter, Header, Depends, HTTPException
from sqlalchemy.orm import Session
from database import get_db
from models import FriendRequest, User
from routers.auth import verify_token
from utils.websocket import active_connections

router = APIRouter()


@router.post("/friends/request/{receiver_id}")
async def send_friend_request(receiver_id: int, authorization: str = Header(...), db: Session = Depends(get_db)):
    """Отправить запрос в друзья"""
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split(" ")[1]
    payload = verify_token(token)
    sender_id = payload.get("user_id")

    if sender_id == receiver_id:
        raise HTTPException(status_code=400, detail="Нельзя добавить себя в друзья")

    counter_request = db.query(FriendRequest).filter(
        FriendRequest.sender_id == receiver_id,
        FriendRequest.receiver_id == sender_id,
        FriendRequest.status == "pending"
    ).first()

    if counter_request:
        # Если есть встречный запрос, сразу принимаем дружбу
        counter_request.status = "accepted"
        db.commit()

        if receiver_id in active_connections:
            await active_connections[receiver_id].send_json({
                "type": "friend_request_response",
                "response": "accepted",
                "from_user_id": sender_id
            })
        return {"message": "Friend request auto-accepted"}

    existing_request = db.query(FriendRequest).filter(
        FriendRequest.sender_id == sender_id,
        FriendRequest.receiver_id == receiver_id
    ).first()

    if existing_request:
        if existing_request.status == "pending":
            raise HTTPException(status_code=400, detail="Friend request already sent")
        elif existing_request.status == "accepted":
            raise HTTPException(status_code=400, detail="You are already friends")
        elif existing_request.status == "rejected":
            # Разрешаем повторную отправку, если заявка была отклонена
            existing_request.status = "pending"
            db.commit()
            return {"message": "Friend request re-sent"}

    friend_request = FriendRequest(sender_id=sender_id, receiver_id=receiver_id)
    db.add(friend_request)
    db.commit()

    if receiver_id in active_connections:
        await active_connections[receiver_id].send_json({
            "type": "friend_request_received",
            "from_user_id": sender_id
        })
    return {"message": "Заявка отправлена"}


@router.post("/friends/respond/{request_id}")
async def respond_to_friend_request(request_id: int, response: str, authorization: str = Header(...),
                              db: Session = Depends(get_db)):
    """Подтвердить или отклонить заявку"""
    if response not in ["accepted", "rejected"]:
        raise HTTPException(status_code=400, detail="Некорректный ответ")

    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split(" ")[1]
    payload = verify_token(token)
    user_id = payload.get("user_id")

    friend_request = db.query(FriendRequest).filter(FriendRequest.id == request_id).first()

    if not friend_request or friend_request.receiver_id != user_id:
        raise HTTPException(status_code=404, detail="Заявка не найдена")

    friend_request.status = response
    db.commit()

    # Уведомление отправителю заявки
    if friend_request.sender_id in active_connections:
        await active_connections[friend_request.sender_id].send_json({
            "type": "friend_request_response",
            "response": response,
            "from_user_id": user_id
        })
    return {"message": f"Заявка {response}"}


@router.get("/friends")
@lru_cache
def get_friends(authorization: str = Header(...), db: Session = Depends(get_db)):
    """Получить список друзей"""
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split(" ")[1]
    payload = verify_token(token)
    user_id = payload.get("user_id")

    friends = (
        db.query(
            FriendRequest.sender_id,
            FriendRequest.receiver_id,
            User.username,
            User.id
        )
        .join(User, (User.id == FriendRequest.sender_id) | (User.id == FriendRequest.receiver_id))
        .filter(
            (FriendRequest.sender_id == user_id) | (FriendRequest.receiver_id == user_id),
            FriendRequest.status == "accepted"
        )
        .all()
    )

    friend_details = []
    for fr in friends:
        if fr.id != user_id:
            friend_details.append({"id": fr.id, "username": fr.username})

    return friend_details


@router.get("/friends/requests")
def get_incoming_friend_requests(authorization: str = Header(...), db: Session = Depends(get_db)):
    """Получить список входящих заявок в друзья."""
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    token = authorization.split(" ")[1]
    payload = verify_token(token)
    user_id = payload.get("user_id")

    requests = (
        db.query(FriendRequest, User.username)
        .join(User, User.id == FriendRequest.sender_id)
        .filter(
            FriendRequest.receiver_id == user_id,
            FriendRequest.status == "pending"
        )
        .all()
    )

    return [
        {"id": request.FriendRequest.id, "sender_id": request.FriendRequest.sender_id, "sender_name": request.username}
        for request in requests
    ]
