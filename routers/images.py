from fastapi import APIRouter
from fastapi.responses import FileResponse
import os

router = APIRouter()
IMAGE_FOLDER = "images"


@router.get("/images/{image_name}")
async def get_image(image_name: str):
    """Получить изображение по имени файла."""
    image_path = os.path.join(IMAGE_FOLDER, image_name)

    # Проверяем, существует ли файл
    if os.path.exists(image_path):
        return FileResponse(image_path)
    else:
        return {"error": "Image not found"}
