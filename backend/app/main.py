from fastapi import FastAPI
from app.config import settings

from app.routers.auth import router as auth_router
from app.routers.scan import router as scan_router

app = FastAPI(
    title=settings.app_name,
    version="0.1.0",
    description="The backend for the MBOA AGRI Mobile app, for plants deseases diognostic and alert"
)

app.include_router(auth_router)
app.include_router(scan_router)


@app.get("/health")
def health_check() -> dict[str, str]:
    return {"status": "Ok", "service": settings.app_name}
