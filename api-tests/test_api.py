import os

import requests


BASE_URL = os.getenv("BASE_URL", "http://127.0.0.1:8080")
TEST_EMAIL = os.getenv("TEST_EMAIL", "residente@test.com")
TEST_PASSWORD = os.getenv("TEST_PASSWORD", "123456")


def test_health():
    response = requests.get(
        f"{BASE_URL}/api/health",
        timeout=10
    )

    assert response.status_code == 200
    assert response.text.strip() == "UP"


def test_login():
    response = requests.post(
        f"{BASE_URL}/api/auth/login",
        json={
            "email": TEST_EMAIL,
            "password": TEST_PASSWORD
        },
        timeout=10
    )

    assert response.status_code == 200