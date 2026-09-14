"""add commune codes, PostGIS geometry and diagnostic analysis tables

Revision ID: 2b3c4d5e6f7a
Revises: 1a2b3c4d5e6f
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from geoalchemy2 import Geography, Geometry


revision: str = "2b3c4d5e6f7a"
down_revision: Union[str, Sequence[str], None] = "1a2b3c4d5e6f"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.execute(
        "CREATE EXTENSION IF NOT EXISTS postgis"
    )

    op.add_column(
        "communes",
        sa.Column(
            "code",
            sa.String(50),
            nullable=True,
        ),
    )

    op.add_column(
        "communes",
        sa.Column(
            "geom",
            Geometry(
                "MULTIPOLYGON",
                srid=4326,
            ),
            nullable=True,
        ),
    )

    op.execute(
        """
        UPDATE communes
        SET code = 'COMMUNE-' || id
        WHERE code IS NULL
        """
    )

    op.alter_column(
        "communes",
        "code",
        nullable=False,
    )

    op.create_unique_constraint(
        "uq_communes_code",
        "communes",
        ["code"],
    )

    op.create_index(
        "ix_communes_code",
        "communes",
        ["code"],
        unique=True,
    )

    op.add_column(
        "pathologies",
        sa.Column(
            "code",
            sa.String(80),
            nullable=True,
        ),
    )

    op.execute(
        """
        UPDATE pathologies
        SET code = 'PATHOLOGY-' || id
        WHERE code IS NULL
        """
    )

    op.alter_column(
        "pathologies",
        "code",
        nullable=False,
    )

    op.create_unique_constraint(
        "uq_pathologies_code",
        "pathologies",
        ["code"],
    )

    op.create_index(
        "ix_pathologies_code",
        "pathologies",
        ["code"],
        unique=True,
    )

    op.create_table(
        "diagnostics",
        sa.Column(
            "id",
            sa.Integer(),
            primary_key=True,
        ),
        sa.Column(
            "user_id",
            sa.Integer(),
            sa.ForeignKey("users.id"),
            nullable=True,
        ),
        sa.Column(
            "pathology_id",
            sa.Integer(),
            sa.ForeignKey("pathologies.id"),
            nullable=True,
        ),
        sa.Column(
            "commune_id",
            sa.Integer(),
            sa.ForeignKey("communes.id"),
            nullable=True,
        ),
        sa.Column(
            "local_id",
            sa.Integer(),
            nullable=True,
        ),
        sa.Column(
            "plant_name",
            sa.String(100),
            nullable=False,
        ),
        sa.Column(
            "disease_name",
            sa.String(150),
            nullable=False,
        ),
        sa.Column(
            "confidence",
            sa.Integer(),
            nullable=False,
        ),
        sa.Column(
            "symptoms",
            sa.Text(),
            nullable=True,
        ),
        sa.Column(
            "treatment_local",
            sa.Text(),
            nullable=True,
        ),
        sa.Column(
            "treatment_chemical",
            sa.Text(),
            nullable=True,
        ),
        sa.Column(
            "timestamp",
            sa.BigInteger(),
            nullable=False,
        ),
        sa.Column(
            "hors_ligne",
            sa.Boolean(),
            nullable=False,
            server_default=sa.false(),
        ),
        sa.Column(
            "latitude",
            sa.Numeric(9, 6),
            nullable=True,
        ),
        sa.Column(
            "longitude",
            sa.Numeric(9, 6),
            nullable=True,
        ),
        sa.Column(
            "position_gps",
            Geography(
                "POINT",
                srid=4326,
            ),
            nullable=True,
        ),
        sa.Column(
            "image_url",
            sa.Text(),
            nullable=True,
        ),
        sa.Column(
            "severity_detected",
            sa.String(20),
            nullable=True,
        ),
        sa.Column(
            "severity_default",
            sa.String(20),
            nullable=True,
        ),
        sa.Column(
            "validation_status",
            sa.String(30),
            nullable=False,
            server_default="PENDING_REVIEW",
        ),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.func.now(),
        ),
    )

    op.create_index(
        "ix_diagnostics_commune_pathology",
        "diagnostics",
        ["commune_id", "pathology_id"],
    )

    op.create_index(
        "ix_diagnostics_created_at",
        "diagnostics",
        ["created_at"],
    )

    op.create_table(
        "pathology_candidates",
        sa.Column(
            "id",
            sa.Integer(),
            primary_key=True,
        ),
        sa.Column(
            "proposed_code",
            sa.String(100),
            nullable=False,
            unique=True,
        ),
        sa.Column(
            "proposed_name",
            sa.String(150),
            nullable=False,
        ),
        sa.Column(
            "plant_name",
            sa.String(100),
            nullable=False,
        ),
        sa.Column(
            "symptoms_ai",
            sa.Text(),
            nullable=True,
        ),
        sa.Column(
            "biological_treatment_ai",
            sa.Text(),
            nullable=True,
        ),
        sa.Column(
            "chemical_treatment_ai",
            sa.Text(),
            nullable=True,
        ),
        sa.Column(
            "severity_detected",
            sa.String(20),
            nullable=True,
        ),
        sa.Column(
            "confidence",
            sa.Float(),
            nullable=True,
        ),
        sa.Column(
            "first_seen_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.func.now(),
        ),
        sa.Column(
            "last_seen_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.func.now(),
        ),
        sa.Column(
            "occurrence_count",
            sa.Integer(),
            nullable=False,
            server_default="1",
        ),
        sa.Column(
            "status",
            sa.String(20),
            nullable=False,
            server_default="PENDING_REVIEW",
        ),
        sa.Column(
            "review_comment",
            sa.Text(),
            nullable=True,
        ),
        sa.Column(
            "validated_pathology_id",
            sa.Integer(),
            nullable=True,
        ),
    )


def downgrade() -> None:
    op.drop_table(
        "pathology_candidates"
    )

    op.drop_index(
        "ix_diagnostics_created_at",
        table_name="diagnostics",
    )

    op.drop_index(
        "ix_diagnostics_commune_pathology",
        table_name="diagnostics",
    )

    op.drop_table(
        "diagnostics"
    )

    op.drop_index(
        "ix_pathologies_code",
        table_name="pathologies",
    )

    op.drop_constraint(
        "uq_pathologies_code",
        "pathologies",
        type_="unique",
    )

    op.drop_column(
        "pathologies",
        "code",
    )

    op.drop_index(
        "ix_communes_code",
        table_name="communes",
    )

    op.drop_constraint(
        "uq_communes_code",
        "communes",
        type_="unique",
    )

    op.drop_column(
        "communes",
        "geom",
    )

    op.drop_column(
        "communes",
        "code",
    )
