const BASE_URL = 'http://localhost:8080';
export async function apiGet(path) {
    const response = await fetch(`${BASE_URL}${path}`);
    if (!response.ok) {
        throw new Error(`GET ${path} falhou: ${response.status} ${response.statusText}`);
    }
    return response.json();
}
export async function apiPost(path, body) {
    const response = await fetch(`${BASE_URL}${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });
    if (!response.ok) {
        const text = await response.text();
        throw new Error(`POST ${path} falhou: ${response.status} - ${text}`);
    }
    return response.json();
}
