import os
import unittest


os.environ["DATABASE_URL"] = "sqlite+pysqlite:///:memory:"


from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.crud.scan import create_or_get_scan
from app.database import Base
from app.models.scan import Scan
from app.models.user import User
from app.schemas.scan import ScanCreateRequest


class ScanSyncTest(unittest.TestCase):
    def setUp(self) -> None:
        self.engine = create_engine(
            "sqlite+pysqlite:///:memory:"
        )

        Base.metadata.create_all(self.engine)

        self.db = Session(self.engine)

        self.user = User(
            username="Agriculteur",
            phone_number="699000001",
            password_hash="not-used-in-this-test",
        )

        self.db.add(self.user)
        self.db.commit()
        self.db.refresh(self.user)

    def tearDown(self) -> None:
        self.db.close()
        self.engine.dispose()

    def scan_payload(
        self,
        server_user_id: int,
    ) -> dict:
        return {
            "user_id": server_user_id,
            "local_id": 27,
            "plant_name": "Maïs",
            "disease_name": "Rouille",
            "confidence": 91,
            "symptoms": "Pustules sur les feuilles",
            "treatment_local": "Retirer les feuilles atteintes",
            "treatment_chemical": "Fongicide homologué",
            "timestamp": 1720000000000,
        }

    def test_room_user_id_is_not_server_identity(self) -> None:
        """
        Vérifie que l'identifiant Room peut être différent
        de l'identifiant PostgreSQL.

        Ici :
        - user_id envoyé par le mobile : 999 ;
        - user_id PostgreSQL authentifié : self.user.id.
        """

        mobile_payload = ScanCreateRequest(
            **self.scan_payload(
                server_user_id=999,
            ),
        )

        result = create_or_get_scan(
            self.db,
            user_id=self.user.id,
            local_id=mobile_payload.local_id,
            plant_name=mobile_payload.plant_name,
            disease_name=mobile_payload.disease_name,
            confidence=mobile_payload.confidence,
            symptoms=mobile_payload.symptoms,
            treatment_local=mobile_payload.treatment_local,
            treatment_chemical=mobile_payload.treatment_chemical,
            timestamp=mobile_payload.timestamp,
        )

        self.assertEqual(
            result.status,
            "created",
        )

        self.assertEqual(
            result.scan.user_id,
            self.user.id,
        )

        self.assertNotEqual(
            mobile_payload.user_id,
            self.user.id,
        )

    def test_retry_returns_same_scan(self) -> None:
        """
        Vérifie qu'une seconde synchronisation du même scan
        ne crée pas de doublon.
        """

        first = create_or_get_scan(
            self.db,
            **self.scan_payload(
                self.user.id,
            ),
        )

        second = create_or_get_scan(
            self.db,
            **self.scan_payload(
                self.user.id,
            ),
        )

        self.assertEqual(
            first.status,
            "created",
        )

        self.assertEqual(
            second.status,
            "already_synced",
        )

        self.assertEqual(
            first.scan.id,
            second.scan.id,
        )

        self.assertEqual(
            self.db.query(Scan).count(),
            1,
        )


if __name__ == "__main__":
    unittest.main()
