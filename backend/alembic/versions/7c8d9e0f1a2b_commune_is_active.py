"""Add communes.is_active used by the mobile contract (GET /api/communes).

Revision ID: 7c8d9e0f1a2b
Revises: 6b7c8d9e0f1a
"""

from alembic import op
import sqlalchemy as sa

revision = "7c8d9e0f1a2b"
down_revision = "6b7c8d9e0f1a"
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Contrat mobile §5 : les communes exposent is_active et les formulaires
    # ne proposent que les communes actives.
    op.add_column(
        "communes",
        sa.Column(
            "is_active",
            sa.Boolean(),
            nullable=False,
            server_default=sa.true(),
        ),
    )
    op.create_index(
        "ix_communes_is_active",
        "communes",
        ["is_active"],
    )


def downgrade() -> None:
    op.drop_index("ix_communes_is_active", table_name="communes")
    op.drop_column("communes", "is_active")
