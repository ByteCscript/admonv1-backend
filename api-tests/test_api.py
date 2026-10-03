import os
from pathlib import Path

import requests


BASE_URL = os.getenv(
    "BASE_URL",
    "http://127.0.0.1:8080"
)

TEST_EMAIL = os.getenv(
    "TEST_EMAIL",
    "residente@test.com"
)

TEST_PASSWORD = os.getenv(
    "TEST_PASSWORD",
    "123456"
)

TIMEOUT = 10


def get_json(response: requests.Response):
    """
    Valida que la respuesta sea JSON y devuelve el body.
    Incluye el body en el error para facilitar el diagnóstico.
    """
    assert response.ok, (
        f"HTTP {response.status_code}: {response.text}"
    )

    return response.json()


def test_create_application_end_to_end():

    session = requests.Session()

    # ============================================================
    # 1. LOGIN
    # ============================================================

    response = session.post(
        f"{BASE_URL}/api/auth/login",
        json={
            "email": TEST_EMAIL,
            "password": TEST_PASSWORD
        },
        timeout=TIMEOUT
    )

    body = get_json(response)

    token = body["data"]["token"]

    assert token

    session.headers.update({
        "Authorization": f"Bearer {token}"
    })


    # ============================================================
    # 2. OBTENER CONVOCATORIAS
    # ============================================================

    response = session.get(
        f"{BASE_URL}/api/calls",
        timeout=TIMEOUT
    )

    body = get_json(response)

    calls = body["data"]

    assert calls, "No existen convocatorias disponibles para el test"


    # ============================================================
    # 3. BUSCAR UNA CONVOCATORIA EN LA QUE PUEDA POSTULARSE
    # ============================================================

    eligible_call = None

    for call in calls:

        call_id = call["id"]

        response = session.get(
            f"{BASE_URL}/api/applications/check",
            params={
                "callId": call_id
            },
            timeout=TIMEOUT
        )

        eligibility_body = get_json(response)

        eligibility = eligibility_body["data"]

        if eligibility["canApply"]:
            eligible_call = call
            break

    assert eligible_call is not None, (
        "El residente no tiene ninguna convocatoria disponible "
        "para realizar una nueva postulación"
    )

    call_id = eligible_call["id"]

    assert call_id


    # ============================================================
    # 4. DOCUMENTOS OBLIGATORIOS
    # ============================================================

    documents = [
        {
            "documentType": "LICENCIA_TRANSITO",
            "fileName": "licencia-transito.pdf"
        },
        {
            "documentType": "SOAT_VIGENTE",
            "fileName": "soat.pdf"
        },
        {
            "documentType": "LICENCIA_CONDUCCION",
            "fileName": "licencia-conduccion.pdf"
        }
    ]

    document_ids = []


    # ============================================================
    # 5. GENERAR PRESIGNED URL + SUBIR + COMPLETAR
    # ============================================================

    for document in documents:

        # --------------------------------------------------------
        # Archivo de prueba
        # --------------------------------------------------------

        file_path = (
            Path(__file__).parent
            / "fixtures"
            / document["fileName"]
        )

        assert file_path.exists(), (
            f"No existe el archivo de prueba: {file_path}"
        )

        file_size = file_path.stat().st_size


        # --------------------------------------------------------
        # 5.1 GENERAR PRESIGNED URL
        # --------------------------------------------------------

        response = session.post(
            f"{BASE_URL}/api/documents/presigned-url",
            json={
                "fileName": document["fileName"],
                "contentType": "application/pdf",
                "size": file_size,
                "documentType": document["documentType"]
            },
            timeout=TIMEOUT
        )

        body = get_json(response)

        presigned = body["data"]

        document_id = presigned["documentId"]
        upload_url = presigned["uploadUrl"]

        assert document_id
        assert upload_url


        # --------------------------------------------------------
        # 5.2 UPLOAD DIRECTO A S3
        # --------------------------------------------------------

        with open(file_path, "rb") as file:

            upload_response = requests.put(
                upload_url,
                data=file,
                headers={
                    "Content-Type": "application/pdf"
                },
                timeout=30
            )

        assert upload_response.status_code in (200, 204), (
            f"Upload S3 falló: "
            f"HTTP {upload_response.status_code}: "
            f"{upload_response.text}"
        )


        # --------------------------------------------------------
        # 5.3 COMPLETAR UPLOAD
        # --------------------------------------------------------

        response = session.post(
            f"{BASE_URL}/api/documents/{document_id}/complete",
            timeout=TIMEOUT
        )

        body = get_json(response)

        assert body["data"]["id"] == str(document_id)

        document_ids.append(document_id)


    # ============================================================
    # 6. CREAR POSTULACIÓN
    # ============================================================

    response = session.post(
        f"{BASE_URL}/api/applications",
        json={
            "callId": call_id,
            "documentIds": document_ids
        },
        timeout=TIMEOUT
    )

    body = get_json(response)

    application = body["data"]

    application_id = application["id"]

    assert application_id
    assert application["callId"] == call_id
    assert application["status"] == "REGISTERED"

    assert len(document_ids) == 3


    # ============================================================
    # 7. CONSULTAR POSTULACIÓN CREADA
    # ============================================================

    response = session.get(
        f"{BASE_URL}/api/applications/{application_id}",
        timeout=TIMEOUT
    )

    body = get_json(response)

    application_detail = body["data"]

    assert application_detail["id"] == application_id
    assert application_detail["callId"] == call_id
    assert application_detail["status"] == "REGISTERED"