from fastapi import FastAPI
from app.config import settings

from app.routers.auth import router as auth_router
from app.routers.scan import router as scan_router
from app.routers.diagnostic import router as diagnostic_router
from app.routers.alerts import router as alert_router

app = FastAPI(
    title=settings.app_name,
    version="0.1.0",
    description="The backend for the MBOA AGRI Mobile app, for plants deseases diognostic and alert"
)

app.include_router(auth_router)
app.include_router(scan_router)
app.include_router(diagnostic_router)
app.include_router(alert_router)


@app.get("/health")
def health_check() -> dict[str, str]:
    return {"status": "Ok", "service": settings.app_name}


@app.get("/")
def root() -> dict[str, str]:
    return {"message": "Welcome to the MBOA AGRI API"}
