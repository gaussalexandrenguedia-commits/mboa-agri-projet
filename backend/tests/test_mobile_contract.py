"""Vérifie que le backend expose exactement le contrat de la doc mobile.

Référence : `mobile-api.pdf` (API MBOA AGRI – Guide pour l'application mobile).
Ces tests ne touchent pas la base : ils inspectent le schéma OpenAPI généré.
"""

import os
import unittest


os.environ.setdefault("DATABASE_URL", "sqlite+pysqlite:///:memory:")


class MobileContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        from app.main import app

        cls.spec = app.openapi()
        cls.paths = cls.spec["paths"]

    def assert_route(self, path: str, method: str) -> dict:
        self.assertIn(path, self.paths, f"Route absente : {path}")
        self.assertIn(
            method.lower(),
            self.paths[path],
            f"Méthode {method} absente sur {path}",
        )
        return self.paths[path][method.lower()]

    def test_authentication_routes(self) -> None:
        self.assert_route("/auth/register", "POST")
        self.assert_route("/auth/login", "POST")

    def test_scan_routes(self) -> None:
        self.assert_route("/api/scans", "POST")
        self.assert_route("/api/scans", "GET")
        self.assert_route("/api/scans/{scan_id}", "GET")
        self.assert_route("/api/scans/diagnose", "POST")

    def test_commune_routes(self) -> None:
        listing = self.assert_route("/api/communes", "GET")
        self.assert_route("/api/communes", "POST")
        self.assert_route("/api/communes/{commune_id}", "PUT")
        self.assert_route("/api/communes/{commune_id}/deactivate", "PATCH")

        params = {p["name"] for p in listing.get("parameters", [])}
        self.assertTrue({"page", "page_size", "is_active"} <= params)

    def test_pathology_routes(self) -> None:
        self.assert_route("/api/pathologies", "GET")
        self.assert_route("/api/pathologies", "POST")
        self.assert_route("/api/pathologies/code/{code}", "GET")
        self.assert_route("/api/pathologies/{pathology_id}", "GET")
        self.assert_route("/api/pathologies/{pathology_id}", "PUT")
        self.assert_route("/api/pathologies/{pathology_id}/deactivate", "PATCH")
        self.assert_route("/api/pathologies/{pathology_id}/activate", "PATCH")

    def test_alert_routes(self) -> None:
        listing = self.assert_route("/api/alerts", "GET")
        self.assert_route("/api/alerts", "POST")

        params = {p["name"] for p in listing.get("parameters", [])}
        self.assertTrue(
            {"commune", "commune_code", "pathology_id", "crop_name", "start_date", "end_date"} <= params
        )

        # GET /api/alerts doit rester public (contrat §7.1)
        self.assertNotIn("security", listing)

    def test_legacy_alert_prefix_kept(self) -> None:
        self.assert_route("/alerts", "GET")

    def test_scan_sync_schema_matches_contract(self) -> None:
        schema = self.spec["components"]["schemas"]["ScanCreateRequest"]
        properties = set(schema["properties"])
        self.assertTrue(
            {
                "local_id",
                "plant_name",
                "disease_name",
                "confidence",
                "symptoms",
                "treatment_local",
                "treatment_chemical",
                "timestamp",
                "hors_ligne",
                "commune_code",
                "latitude",
                "longitude",
            } <= properties
        )

    def test_commune_page_schema(self) -> None:
        schema = self.spec["components"]["schemas"]["CommunePageResponse"]
        self.assertEqual(
            {"items", "total", "page", "page_size", "pages"},
            set(schema["properties"]),
        )


if __name__ == "__main__":
    unittest.main()
