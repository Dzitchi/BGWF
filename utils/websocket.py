from fastapi import WebSocket, WebSocketDisconnect, APIRouter
from typing import Dict

# Хранилище подключений: user_id -> WebSocket
active_connections: Dict[int, WebSocket] = {}

router = APIRouter()


@router.websocket("/ws/{user_id}")
async def websocket_endpoint(websocket: WebSocket, user_id: int):
    await websocket.accept()
    active_connections[user_id] = websocket
    try:
        while True:
            await websocket.receive_text()  # Держим соединение открытым
    except WebSocketDisconnect:
        active_connections.pop(user_id, None)
