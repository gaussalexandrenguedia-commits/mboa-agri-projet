from fastapi import APIRouter, File, Form, UploadFile


router = APIRouter(
    prefix="/api/scans",
    tags=["Diagnostics"],
)


@router.post("/diagnose")
async def diagnose_scan(
    image: UploadFile = File(...),
    plant_name: str = Form(...),
    symptoms: str = Form(default=""),
    latitude: float | None = Form(default=None),
    longitude: float | None = Form(default=None),
) -> dict[str, str | float | None]:
    return {
        "message": "Image reçue. Le service Gemini sera ajouté à l’étape suivante.",
        "filename": image.filename,
        "plant_name": plant_name,
        "symptoms": symptoms,
        "latitude": latitude,
        "longitude": longitude,
    }
