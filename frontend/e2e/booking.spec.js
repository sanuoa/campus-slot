import { test, expect } from '@playwright/test';

// Frontend interaction tests use explicit API fixtures. Backend behaviour has its own integration suite.
async function fixture(page, { role = 'STUDENT', quota = false, authenticated = true } = {}) {
  const date = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Colombo', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
  const next = new Date(`${date}T12:00:00+05:30`); next.setUTCDate(next.getUTCDate() + 1);
  const tomorrow = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Colombo', year: 'numeric', month: '2-digit', day: '2-digit' }).format(next);
  const user = { id: 1, name: 'Test Student', email: 'test@iit.ac.lk', role };
  const facilities = [{ id: 1, name: 'Test Study Room', type: 'STUDY_ROOM', location: 'Test building', capacity: 6, active: true }, { id: 2, name: 'Test Computer Lab', type: 'COMPUTER_LAB', location: 'Test building', capacity: 20, active: true }, { id: 3, name: 'Test Sports Court', type: 'SPORTS_COURT', location: 'Test sports area', capacity: 10, active: true }];
  let bookings = quota ? [{ id: 1, date: tomorrow, facilityName: 'Test Study Room', status: 'COMPLETED', startsAt: `${tomorrow}T08:00:00+05:30`, endsAt: `${tomorrow}T09:00:00+05:30` }] : [];
  const mutations = [];
  await page.route('**/api/**', async route => {
    const request = route.request(); const path = new URL(request.url()).pathname;
    let result = null; let status = 200;
    if (path === '/api/auth/csrf') result = { token: 'fixture-token', headerName: 'X-CSRF-TOKEN' };
    else if (path === '/api/auth/me') { if (!authenticated) { status = 401; result = { message: 'Please log in.' }; } else result = user; }
    else if (path === '/api/auth/login') { authenticated = true; status = 204; }
    else if (path === '/api/auth/register') { mutations.push(request.postDataJSON()); status = 201; }
    else if (path === '/api/auth/logout') { authenticated = false; status = 204; }
    else if (path === '/api/facilities') result = facilities;
    else if (path === '/api/bookings/mine') result = bookings;
    else if (/\/facilities\/\d+\/slots$/.test(path)) result = [{ id: 10, startsAt: `${tomorrow}T09:00:00+05:30`, endsAt: `${tomorrow}T10:00:00+05:30`, state: bookings.some(b => b.status === 'CONFIRMED') ? 'BOOKED' : 'AVAILABLE' }];
    else if (path === '/api/bookings' && request.method() === 'POST') { mutations.push(request.postDataJSON()); const booking = { id: 1, date: tomorrow, facilityName: 'Test Study Room', status: 'CONFIRMED', startsAt: `${tomorrow}T09:00:00+05:30`, endsAt: `${tomorrow}T10:00:00+05:30` }; bookings = [booking]; result = booking; status = 201; }
    else if (path === '/api/admin/bookings') result = [];
    else if (path.startsWith('/api/admin/facilities')) { mutations.push(request.postDataJSON()); result = facilities[0]; status = 201; }
    else { status = 404; result = { message: `No fixture for ${path}` }; }
    await route.fulfill({ status, contentType: 'application/json', body: status === 204 ? undefined : JSON.stringify(result) });
  });
  return { tomorrow, mutations };
}

test('student confirms a whole-facility session and sees the booking', async ({ page }) => {
  const { tomorrow, mutations } = await fixture(page);
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Make room for your next idea.' })).toBeVisible();
  await page.getByLabel('Booking date').fill(tomorrow);
  await page.getByRole('button', { name: /09:00 am.*available/i }).click();
  await expect(page.getByRole('dialog')).toContainText('Entire facility');
  await page.getByRole('button', { name: 'Confirm booking' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Your space is booked' })).toBeVisible();
  expect(mutations).toEqual([{ slotId: 10 }]);
  await page.getByRole('button', { name: /My bookings/ }).click();
  await expect(page.locator('.booking-row')).toContainText('Test Study Room');
});

test('a completed session keeps the daily allowance used', async ({ page }) => {
  const { tomorrow } = await fixture(page, { quota: true });
  await page.goto('/'); await page.getByLabel('Booking date').fill(tomorrow);
  await expect(page.getByText('You already have a session for this date.')).toBeVisible();
  await expect(page.getByRole('button', { name: /09:00 am.*available/i })).toBeDisabled();
});

test('registration uses IIT email and does not request administrator privileges', async ({ page }) => {
  const { mutations } = await fixture(page, { authenticated: false });
  await page.goto('/'); await page.getByRole('button', { name: 'Create an account' }).click();
  await page.getByLabel('Full name').fill('Test Student'); await page.getByLabel('IIT email').fill('test@iit.ac.lk');
  await page.getByLabel('Password', { exact: true }).fill('A test password 123!');
  await page.getByRole('button', { name: 'Create account →' }).click();
  await expect(page.getByRole('status')).toContainText('Account created.');
  expect(mutations[0]).toEqual({ name: 'Test Student', email: 'test@iit.ac.lk', password: 'A test password 123!' });
});

test('administrator can add a facility and mobile layout has no horizontal overflow', async ({ page }) => {
  const { mutations } = await fixture(page, { role: 'ADMIN' });
  await page.setViewportSize({ width: 390, height: 844 }); await page.goto('/');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.getByRole('button', { name: 'Administration' }).click();
  await page.getByLabel('Name', { exact: true }).fill('New Lab'); await page.getByLabel('Location').fill('Level 2');
  await page.getByLabel('Capacity').fill('12'); await page.getByRole('button', { name: 'Save facility' }).click();
  await expect(page.getByRole('status')).toContainText('Facility saved.');
  expect(mutations[0]).toMatchObject({ name: 'New Lab', location: 'Level 2', capacity: 12 });
});
