const BASE_URL = 'http://localhost:8080';

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`);
  if (!response.ok) {
    throw new Error(`GET ${path} falhou: ${response.status} ${response.statusText}`);
  }
  return response.json() as Promise<T>;
}

export async function apiPatch<T>(path: string): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, { method: 'PATCH' });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`PATCH ${path} falhou: ${response.status} - ${text}`);
  }
  return response.json() as Promise<T>;
}

export async function apiPut<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`PUT ${path} falhou: ${response.status} - ${text}`);
  }
  return response.json() as Promise<T>;
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`POST ${path} falhou: ${response.status} - ${text}`);
  }
  return response.json() as Promise<T>;
}
