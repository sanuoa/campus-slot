export class ApiError extends Error {
  constructor(message, status) { super(message); this.status = status; }
}

export async function api(path, { method = 'GET', body } = {}) {
  const headers = {};
  if (method !== 'GET') {
    const response = await fetch('/api/auth/csrf', { credentials: 'same-origin', cache: 'no-store' });
    if (!response.ok) throw new ApiError('Unable to prepare a secure request. Refresh and try again.', response.status);
    const csrf = await response.json();
    headers[csrf.headerName] = csrf.token;
  }
  if (body && !(body instanceof URLSearchParams)) headers['Content-Type'] = 'application/json';
  const response = await fetch(`/api${path}`, {
    method, credentials: 'same-origin', cache: 'no-store', headers,
    body: body instanceof URLSearchParams ? body : body ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new ApiError(data.message || 'Something went wrong. Please try again.', response.status);
  }
  return response.status === 204 || response.headers.get('content-length') === '0' ? null : response.json().catch(() => null);
}
