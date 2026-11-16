"""Persist profile fields and add administrator role."""
from alembic import op
import sqlalchemy as sa

revision = "5a6b7c8d9e0f"
down_revision = "4c5d6e7f8a9b"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("users", sa.Column("is_admin", sa.Boolean(), nullable=False, server_default=sa.false()))
    op.add_column("users", sa.Column("commune", sa.String(120), nullable=False, server_default=""))
    op.add_column("users", sa.Column("cultures", sa.String(500), nullable=False, server_default=""))
    op.add_column("users", sa.Column("langue", sa.String(10), nullable=False, server_default="fr"))
    op.add_column("users", sa.Column("consentement_alertes", sa.Boolean(), nullable=False, server_default=sa.false()))


def downgrade() -> None:
    for column in ("consentement_alertes", "langue", "cultures", "commune", "is_admin"):
        op.drop_column("users", column)
