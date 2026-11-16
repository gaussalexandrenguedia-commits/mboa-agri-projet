"""Gemini-backed compatibility endpoint for agronomy chat and tutoring."""
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel

from app.core.dependencies import get_current_user
from app.models.user import User
from app.services.gemini import GeminiService, GeminiServiceError

router = APIRouter(prefix="/api/ai", tags=["AI"])


class Part(BaseModel):
    text: str | None = None


class Content(BaseModel):
    parts: list[Part]
    role: str | None = None


class GenerateRequest(BaseModel):
    contents: list[Content]
    systemInstruction: Content | None = None
    generationConfig: dict[str, Any] | None = None


@router.post("/generate")
def generate(payload: GenerateRequest, _: User = Depends(get_current_user)):
    if not payload.contents:
        raise HTTPException(status_code=422, detail="La conversation est vide.")
    try:
        contents = []
        # Older Android payloads omit Gemini roles. Chat history is emitted in
        # alternating assistant/user order and starts with an assistant greeting.
        for index, item in enumerate(payload.contents):
            role = item.role or ("model" if len(payload.contents) > 1 and index % 2 == 0 else "user")
            if role not in {"user", "model"}:
                role = "user"
            contents.append({"role": role, "parts": [part.model_dump(exclude_none=True) for part in item.parts]})
        text = GeminiService().generate_text(
            contents=contents,
            system_instruction="\n".join(p.text or "" for p in (payload.systemInstruction.parts if payload.systemInstruction else [])),
            generation_config=payload.generationConfig,
        )
    except GeminiServiceError as exc:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail=str(exc)) from exc
    return {"candidates": [{"content": {"parts": [{"text": text}]}}]}
