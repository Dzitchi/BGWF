from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from database import engine, Base
from routers import auth, games, users, friends, images
from utils import websocket

Base.metadata.create_all(bind=engine)

app = FastAPI()
app.include_router(websocket.router)
app.include_router(auth.router)
app.include_router(games.router)
app.include_router(users.router)
app.include_router(friends.router)


# Папка с изображениями
IMAGE_FOLDER = "images"

app.include_router(images.router)

# Монтируем папку с изображениями как статический ресурс
app.mount("/static", StaticFiles(directory=IMAGE_FOLDER), name="static")
