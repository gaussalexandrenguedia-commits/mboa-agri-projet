from dataclasses import dataclass
from datetime import datetime, timezone
import unicodedata

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.pathology import Pathology
from app.models.pathology_candidate import PathologyCandidate
from app.services.gemini import GeminiPrediction


@dataclass
class DiagnosticResolution:
    pathology_id: int | None
    pathology_code: str | None
    disease_name: str
    symptoms: str | None
    treatment_local: str | None
    treatment_chemical: str | None
    severity_detected: str
    severity_default: str | None
    information_source: str
    validation_status: str


def get_catalog_for_plant(
    db: Session,
    plant_name: str,
) -> list[dict[str, str]]:
    statement = (
        select(Pathology)
        .where(
            Pathology.is_active.is_(True),
            Pathology.crop_name.ilike(
                plant_name.strip()
            ),
        )
        .order_by(
            Pathology.code.asc()
        )
    )

    pathologies = list(
        db.scalars(statement).all()
    )

    return [
        {
            "code": pathology.code,
            "common_name": pathology.common_name,
            "crop_name": pathology.crop_name,
        }
        for pathology in pathologies
    ]


def resolve_prediction(
    db: Session,
    prediction: GeminiPrediction,
    plant_name: str,
) -> DiagnosticResolution:
    pathology = None

    if prediction.pathology_code:
        statement = select(Pathology).where(
            Pathology.code
            == prediction.pathology_code.strip(),
            Pathology.is_active.is_(True),
        )

        pathology = db.scalar(statement)

    if pathology is not None:
        return DiagnosticResolution(
            pathology_id=pathology.id,
            pathology_code=pathology.code,
            disease_name=pathology.common_name,
            symptoms=pathology.key_symptoms,
            treatment_local=pathology.biological_treatment,
            treatment_chemical=pathology.chemical_treatment,
            severity_detected=prediction.severity_detected,
            severity_default=pathology.default_severity,
            information_source="CATALOG",
            validation_status="CATALOG_MATCH",
        )

    candidate_code = build_candidate_code(
        plant_name=plant_name,
        disease_name=prediction.disease_name,
    )

    save_or_update_candidate(
        db=db,
        candidate_code=candidate_code,
        prediction=prediction,
        plant_name=plant_name,
    )

    return DiagnosticResolution(
        pathology_id=None,
        pathology_code=candidate_code,
        disease_name=prediction.disease_name,
        symptoms=prediction.symptoms_ai,
        treatment_local=prediction.treatment_local_ai,
        treatment_chemical=prediction.treatment_chemical_ai,
        severity_detected=prediction.severity_detected,
        severity_default=None,
        information_source="AI_PROVISIONAL",
        validation_status="AI_ONLY",
    )


def build_candidate_code(
    plant_name: str,
    disease_name: str,
) -> str:
    plant_code = normalize_code_part(
        plant_name
    )

    disease_code = normalize_code_part(
        disease_name
    )

    return (
        f"CANDIDATE_{plant_code}_{disease_code}"
    )[:100]


def normalize_code_part(
    value: str,
) -> str:
    normalized = unicodedata.normalize(
        "NFKD",
        value,
    )

    normalized = normalized.encode(
        "ascii",
        "ignore",
    ).decode("ascii")

    normalized = "".join(
        character
        if character.isalnum()
        else "_"
        for character in normalized.upper()
    )

    while "__" in normalized:
        normalized = normalized.replace(
            "__",
            "_",
        )

    return normalized.strip("_") or "UNKNOWN"


def save_or_update_candidate(
    db: Session,
    candidate_code: str,
    prediction: GeminiPrediction,
    plant_name: str,
) -> PathologyCandidate:
    statement = select(
        PathologyCandidate
    ).where(
        PathologyCandidate.proposed_code
        == candidate_code
    )

    candidate = db.scalar(statement)

    if candidate is None:
        candidate = PathologyCandidate(
            proposed_code=candidate_code,
            proposed_name=prediction.disease_name,
            plant_name=plant_name,
            symptoms_ai=prediction.symptoms_ai,
            biological_treatment_ai=(
                prediction.treatment_local_ai
            ),
            chemical_treatment_ai=(
                prediction.treatment_chemical_ai
            ),
            severity_detected=(
                prediction.severity_detected
            ),
            confidence=prediction.confidence,
            occurrence_count=1,
            status="PENDING_REVIEW",
        )

        db.add(candidate)

    else:
        candidate.last_seen_at = datetime.now(
            timezone.utc
        )

        candidate.occurrence_count += 1
        candidate.confidence = prediction.confidence
        candidate.severity_detected = (
            prediction.severity_detected
        )

        if prediction.symptoms_ai:
            candidate.symptoms_ai = (
                prediction.symptoms_ai
            )

        if prediction.treatment_local_ai:
            candidate.biological_treatment_ai = (
                prediction.treatment_local_ai
            )

        if prediction.treatment_chemical_ai:
            candidate.chemical_treatment_ai = (
                prediction.treatment_chemical_ai
            )

    db.flush()

    return candidate
