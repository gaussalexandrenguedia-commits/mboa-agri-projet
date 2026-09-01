from fastapi import FastAPI
from app.config import settings

app = FastAPI(
    title=settings.app_name,
    version="0.1.0",
    description="The backend for the MBOA AGRI Mobile app, for plants deseases diognostic and alert"
)


@app.get("/health")
def health_check() -> dict[str, str]:
    return {"status": "Ok", "service": settings.app_name}
