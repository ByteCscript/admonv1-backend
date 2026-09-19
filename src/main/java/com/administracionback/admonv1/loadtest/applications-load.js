
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const status400 = new Counter('status_400');
const status401 = new Counter('status_401');
const status403 = new Counter('status_403');
const status409 = new Counter('status_409');
const status500 = new Counter('status_500');
const statusOther = new Counter('status_other');


const BASE_URL = 'http://100.26.48.222:8080';

export const options = {
    scenarios: {
        applications: {
            executor: 'per-vu-iterations',
            vus: 300,
            iterations: 1,
            maxDuration: '2m',
        },
    },

    thresholds: {
        http_req_duration: ['p(95)<2000'],
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {

    const residentId = __VU;

    const payload = JSON.stringify({
        callId: 1,
        residentId: residentId,
        documentIds: [],
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const response = http.post(
        `${BASE_URL}/api/applications`,
        payload,
        params
    );

    switch (response.status) {
        case 400:
            status400.add(1);
            break;
        case 401:
            status401.add(1);
            break;
        case 403:
            status403.add(1);
            break;
        case 409:
            status409.add(1);
            break;
        case 500:
            status500.add(1);
            break;
        default:
            if (response.status !== 200) {
                statusOther.add(1);
            }
    }

    if (response.status !== 200) {
        console.log(
            `Resident ${residentId} -> HTTP ${response.status} -> ${response.body}`
        );
    }

    check(response, {
        'HTTP 200': (r) => r.status === 200,
    });
}