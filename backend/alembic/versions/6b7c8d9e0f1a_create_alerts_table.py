"""Create territorial alerts table used by alert endpoints and automation."""
from alembic import op
import sqlalchemy as sa

revision = "6b7c8d9e0f1a"
down_revision = "5a6b7c8d9e0f"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "alerts",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("pathology_id", sa.Integer(), sa.ForeignKey("pathologies.id"), nullable=False),
        sa.Column("commune_id", sa.Integer(), sa.ForeignKey("communes.id"), nullable=False),
        sa.Column("scan_count", sa.Integer(), nullable=False),
        sa.Column("alert_level", sa.String(20), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column("period_start", sa.DateTime(timezone=True), nullable=True),
        sa.Column("period_end", sa.DateTime(timezone=True), nullable=True),
        sa.Column("last_scan_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("status", sa.String(20), nullable=False, server_default="ACTIVE"),
        sa.Column("resolved_at", sa.DateTime(timezone=True), nullable=True),
    )
    op.create_index("ix_alerts_commune_pathology_status", "alerts", ["commune_id", "pathology_id", "status"])


def downgrade() -> None:
    op.drop_index("ix_alerts_commune_pathology_status", table_name="alerts")
    op.drop_table("alerts")
