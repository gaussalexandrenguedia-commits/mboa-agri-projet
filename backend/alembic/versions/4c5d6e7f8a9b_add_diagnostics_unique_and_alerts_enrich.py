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

    # L'ancienne version tentait un ALTER sur alerts même quand la table
    # n'existait pas, ce qui laisse PostgreSQL en transaction aborted. La
    # migration 6b7c8d9e0f1a crée désormais la table complète. On enrichit
    # uniquement les installations historiques qui possèdent déjà alerts.
    inspector = sa.inspect(op.get_bind())
    if inspector.has_table("alerts"):
        existing_columns = {column["name"] for column in inspector.get_columns("alerts")}
        columns = (
            ("period_start", sa.DateTime(timezone=True), True),
            ("period_end", sa.DateTime(timezone=True), True),
            ("last_scan_at", sa.DateTime(timezone=True), True),
            ("status", sa.String(20), False),
            ("resolved_at", sa.DateTime(timezone=True), True),
        )
        for name, column_type, nullable in columns:
            if name not in existing_columns:
                op.add_column(
                    "alerts",
                    sa.Column(name, column_type, nullable=nullable,
                              **({"server_default": "ACTIVE"} if name == "status" else {})),
                )
        existing_indexes = {index["name"] for index in inspector.get_indexes("alerts")}
        if "ix_alerts_commune_pathology_status" not in existing_indexes:
            op.create_index(
                "ix_alerts_commune_pathology_status",
                "alerts",
                ["commune_id", "pathology_id", "status"],
            )


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
