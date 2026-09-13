"""add unique user/local id for offline scan synchronization

Revision ID: 1a2b3c4d5e6f
Revises: d90f213c1795
"""
from typing import Sequence, Union

from alembic import op


revision: str = "1a2b3c4d5e6f"
down_revision: Union[str, Sequence[str], None] = "d90f213c1795"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_unique_constraint(
        "uq_scans_user_local_id",
        "scans",
        ["user_id", "local_id"],
    )


def downgrade() -> None:
    op.drop_constraint("uq_scans_user_local_id", "scans", type_="unique")
