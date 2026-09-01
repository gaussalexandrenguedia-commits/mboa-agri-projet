from fastapi import FastAPI


app = FastAPI(
    title="MBOA AGRI API",
    version="0.1.0",
    description="The backend for the MBOA AGRI Mobile app, for plants deseases diognostic and alert"
)


@app.get("/health")
def health_check() -> dict[str, str]:
    return {"status": "Ok", "service": "Mboa agri api"}
