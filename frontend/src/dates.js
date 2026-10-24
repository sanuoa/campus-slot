const zone = 'Asia/Colombo';
export const campusDate = (value = new Date()) => new Intl.DateTimeFormat('en-CA', { timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(value));
export const time = value => new Intl.DateTimeFormat('en-GB', { timeZone: zone, hour: '2-digit', minute: '2-digit', hour12: true }).format(new Date(value));
export const dayLabel = value => new Intl.DateTimeFormat('en-GB', { timeZone: zone, weekday: 'short', day: 'numeric', month: 'short' }).format(new Date(`${value}T12:00:00+05:30`));
export function maxDate() { const date = new Date(`${campusDate()}T12:00:00+05:30`); date.setUTCDate(date.getUTCDate() + 30); return campusDate(date); }
export const hasDailyBooking = (bookings, date) => bookings.some(b => b.date === date && b.status !== 'CANCELLED');
export const labels = { STUDY_ROOM: 'Study rooms', COMPUTER_LAB: 'Computer labs', SPORTS_COURT: 'Sports courts' };
