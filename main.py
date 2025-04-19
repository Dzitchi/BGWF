import logging
from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from database import engine, Base
from routers import auth, games, users, friends, images, groups
from utils import websocket
from database import SessionLocal
from models import Group, GroupMember, GroupInvitation
from datetime import datetime, timedelta
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.interval import IntervalTrigger

Base.metadata.create_all(bind=engine)

app = FastAPI()
app.include_router(websocket.router)
app.include_router(auth.router)
app.include_router(games.router)
app.include_router(users.router)
app.include_router(friends.router)
app.include_router(groups.router)


# Папка с изображениями
IMAGE_FOLDER = "images"

app.include_router(images.router)

# Монтируем папку с изображениями как статический ресурс
app.mount("/static", StaticFiles(directory=IMAGE_FOLDER), name="static")


# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler("app.log"),  # Логи в файл
        logging.StreamHandler()          # Логи в консоль
    ]
)
logger = logging.getLogger(__name__)


def delete_old_groups():
    """
    Удаляет группы, созданные более 24 часов назад, вместе с участниками и приглашениями.
    """
    logger.info("Starting the process of deleting obsolete groups.")
    db = SessionLocal()
    try:
        cutoff_time = datetime.utcnow() - timedelta(minutes=10)
        old_groups = db.query(Group).filter(Group.created_at < cutoff_time).all()
        for group in old_groups:
            try:
                # Удаляем участников группы
                db.query(GroupMember).filter(GroupMember.group_id == group.id).delete()
                # Удаляем приглашения
                db.query(GroupInvitation).filter(GroupInvitation.group_id == group.id).delete()
                # Удаляем саму группу
                db.delete(group)
                db.commit()
                logger.info(f"Group with ID {group.id} has been deleted.")
            except Exception as e:
                db.rollback()
                logger.error(f"Error deleting group with ID {group.id}: {e}")
    except Exception as e:
        logger.error(f"Error getting stale groups: {e}")
    finally:
        db.close()
        logger.info("Completing the process of deleting obsolete groups.")


# Настраиваем планировщик
scheduler = BackgroundScheduler()
scheduler.add_job(
    func=delete_old_groups,
    trigger=IntervalTrigger(minutes=5),  # Проверяем каждые 10 минут
    id='delete_old_groups',
    name='Delete groups older than 24 hours',
    replace_existing=True
)


# Запускаем планировщик при старте приложения
@app.on_event("startup")
async def startup_event():
    scheduler.start()
    logger.info("scheduler started.")


# Останавливаем планировщик при завершении приложения
@app.on_event("shutdown")
async def shutdown_event():
    scheduler.shutdown()
    logger.info("scheduler stopped.")
