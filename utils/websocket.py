import json
from fastapi import WebSocket, WebSocketDisconnect, APIRouter
from typing import Dict
from models import GroupMember
from database import get_db

# Хранилище подключений: user_id -> WebSocket
active_connections: Dict[int, WebSocket] = {}

router = APIRouter()


@router.websocket("/ws/{user_id}")
async def websocket_endpoint(websocket: WebSocket, user_id: int):
    await websocket.accept()
    active_connections[user_id] = websocket
    try:
        while True:
            data = await websocket.receive_text()
            message = json.loads(data)
            if message.get("type") == "group_filters_updated":
                group_id = message.get("group_id")
                # Получаем всех участников группы
                db = next(get_db())
                member_ids = [m.user_id for m in db.query(GroupMember).filter(GroupMember.group_id == group_id).all()]
                db.close()
                # Отправляем обновление всем участникам группы
                for member_id in member_ids:
                    if member_id in active_connections:
                        await active_connections[member_id].send_json(message)
    except WebSocketDisconnect:
        active_connections.pop(user_id, None)
