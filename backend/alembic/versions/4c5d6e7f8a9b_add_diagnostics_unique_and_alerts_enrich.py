"""add diagnostics unique constraint for offline idempotence and enrich alerts

Revision ID: 4c5d6e7f8a9b
Revises: 2b3c4d5e6f7a
"""

from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa

revision: str = "4c5d6e7f8a9b"
down_revision: Union[str, Sequence[str], None] = "2b3c4d5e6f7a"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Idempotence diagnostics : user_id + local_id + hors_ligne
    # Permet d'éviter les doublons lors de la synchronisation offline
    # local_id peut être null pour les diagnostics online purs, donc contrainte partielle
    op.create_unique_constraint(
        "uq_diagnostics_user_local_hors_ligne",
        "diagnostics",
        ["user_id", "local_id", "hors_ligne"],
    )

    # Enrichir alerts avec cycle de vie (recommandé dans le rapport)
    # Vérifier si table alerts existe
    try:
        op.add_column(
            "alerts",
            sa.Column("period_start", sa.DateTime(timezone=True), nullable=True),
        )
        op.add_column(
            "alerts",
            sa.Column("period_end", sa.DateTime(timezone=True), nullable=True),
        )
        op.add_column(
            "alerts",
            sa.Column("last_scan_at", sa.DateTime(timezone=True), nullable=True),
        )
        op.add_column(
            "alerts",
            sa.Column("status", sa.String(20), nullable=False, server_default="ACTIVE"),
        )
        op.add_column(
            "alerts",
            sa.Column("resolved_at", sa.DateTime(timezone=True), nullable=True),
        )
        op.create_index(
            "ix_alerts_commune_pathology_status",
            "alerts",
            ["commune_id", "pathology_id", "status"],
        )
    except Exception:
        # Si table alerts n'existe pas encore ou colonnes déjà présentes, ignorer
        pass


def downgrade() -> None:
    try:
        op.drop_index("ix_alerts_commune_pathology_status", table_name="alerts")
        op.drop_column("alerts", "resolved_at")
        op.drop_column("alerts", "status")
        op.drop_column("alerts", "last_scan_at")
        op.drop_column("alerts", "period_end")
        op.drop_column("alerts", "period_start")
    except Exception:
        pass

    op.drop_constraint(
        "uq_diagnostics_user_local_hors_ligne",
        "diagnostics",
        type_="unique",
    )
