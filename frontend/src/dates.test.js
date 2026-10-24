import { describe, it, expect } from 'vitest';
import { campusDate, hasDailyBooking, time } from './dates';

describe('Campus booking dates', () => {
  it('uses the Sri Lanka day when UTC is still the previous day', () => {
    expect(campusDate('2026-10-24T20:00:00Z')).toBe('2026-10-25');
  });
  it('counts completed sessions towards the daily quota across all facilities', () => {
    expect(hasDailyBooking([{ date: '2026-10-25', status: 'COMPLETED', facilityType: 'STUDY_ROOM' }], '2026-10-25')).toBe(true);
  });
  it('restores quota for a cancelled booking but keeps other days separate', () => {
    expect(hasDailyBooking([{ date: '2026-10-25', status: 'CANCELLED' }, { date: '2026-10-26', status: 'CONFIRMED' }], '2026-10-25')).toBe(false);
  });
  it('renders time in the campus zone, regardless of the browser zone', () => {
    expect(time('2026-10-25T03:30:00Z').toLowerCase()).toContain('09:00');
  });
});
