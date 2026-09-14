import json
import os
import sys

import httpx
from dotenv import load_dotenv


load_dotenv( )


api_key = os.getenv("GEMINI_API_KEY")
model = os.getenv(
    "GEMINI_MODEL",
    "gemini-2.0-flash",
)


if not api_key:
    print(
        "ERREUR : GEMINI_API_KEY est absente "
        "du fichier .env."
    )
    sys.exit(1)


endpoint = (
    "https://generativelanguage.googleapis.com/"
    f"v1beta/models/{model}:generateContent"
 )


payload = {
    "contents": [
        {
            "parts": [
                {
                    "text": (
                        "Réponds uniquement par : "
                        "Connexion Gemini OK"
                    ),
                }
            ],
        }
    ],
    "generationConfig": {
        "temperature": 0.1,
        "maxOutputTokens": 50,
    },
}


headers = {
    "Content-Type": "application/json",
    "x-goog-api-key": api_key,
}


print("Modèle utilisé :", model)
print("Endpoint :", endpoint)
print("Clé API détectée : oui")
print("Envoi de la requête à Gemini...")


try:
    response = httpx.post(
        endpoint,
        headers=headers,
        json=payload,
        timeout=60.0,
     )

except httpx.HTTPError as error:
    print( )
    print("ERREUR RÉSEAU :")
    print(str(error))
    sys.exit(1)


print()
print("Code HTTP :", response.status_code)


try:
    response_data = response.json()
except ValueError:
    print("Réponse non JSON reçue :")
    print(response.text)
    sys.exit(1)


if response.status_code >= 400:
    print()
    print("ERREUR RETOURNÉE PAR GEMINI :")
    print(
        json.dumps(
            response_data,
            indent=2,
            ensure_ascii=False,
        )
    )
    sys.exit(1)


print()
print("Réponse complète de Gemini :")
print(
    json.dumps(
        response_data,
        indent=2,
        ensure_ascii=False,
    )
)


try:
    text = (
        response_data["candidates"][0]
        ["content"]["parts"][0]
        ["text"]
    )

    print()
    print("Réponse extraite :")
    print(text)

except (KeyError, IndexError, TypeError) as error:
    print()
    print(
        "Gemini a répondu, mais le format "
        "de la réponse est inattendu."
    )
    print("Détail :", str(error))
    sys.exit(1)

