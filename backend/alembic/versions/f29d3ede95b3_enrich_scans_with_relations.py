"""enrich scans with relations

Revision ID: f29d3ede95b3
Revises: 3d7996cf0e45
Create Date: 2026-09-04 21:40:32.514592

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'f29d3ede95b3'
down_revision: Union[str, Sequence[str], None] = '3d7996cf0e45'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table(
        "communes",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("name", sa.String(length=100), nullable=False),
        sa.Column("postal_code", sa.String(length=20), nullable=True),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_communes_name"),
        "communes",
        ["name"],
        unique=True,
    )

    op.create_table(
        "pathologies",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("technical_name", sa.String(length=100), nullable=True),
        sa.Column("common_name", sa.String(length=100), nullable=False),
        sa.Column("crop_name", sa.String(length=50), nullable=False),
        sa.Column("key_symptoms", sa.Text(), nullable=True),
        sa.Column("biological_treatment", sa.Text(), nullable=True),
        sa.Column("chemical_treatment", sa.Text(), nullable=True),
        sa.Column("default_severity", sa.String(length=20), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )

    op.add_column(
        "scans",
        sa.Column("user_id", sa.Integer(), nullable=True),
    )
    op.add_column(
        "scans",
        sa.Column("pathology_id", sa.Integer(), nullable=True),
    )
    op.add_column(
        "scans",
        sa.Column("commune_id", sa.Integer(), nullable=True),
    )
    op.add_column(
        "scans",
        sa.Column(
            "hors_ligne",
            sa.Boolean(),
            nullable=False,
            server_default=sa.false(),
        ),
    )

    op.create_foreign_key(
        "fk_scans_user_id_users",
        "scans",
        "users",
        ["user_id"],
        ["id"],
    )
    op.create_foreign_key(
        "fk_scans_pathology_id_pathologies",
        "scans",
        "pathologies",
        ["pathology_id"],
        ["id"],
    )
    op.create_foreign_key(
        "fk_scans_commune_id_communes",
        "scans",
        "communes",
        ["commune_id"],
        ["id"],
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_constraint(
        "fk_scans_commune_id_communes",
        "scans",
        type_="foreignkey",
    )
    op.drop_constraint(
        "fk_scans_pathology_id_pathologies",
        "scans",
        type_="foreignkey",
    )
    op.drop_constraint(
        "fk_scans_user_id_users",
        "scans",
        type_="foreignkey",
    )

    op.drop_column("scans", "hors_ligne")
    op.drop_column("scans", "commune_id")
    op.drop_column("scans", "pathology_id")
    op.drop_column("scans", "user_id")

    op.drop_table("pathologies")
    op.drop_index(op.f("ix_communes_name"), table_name="communes")
    op.drop_table("communes")
