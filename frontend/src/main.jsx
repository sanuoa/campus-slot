import React, { useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { api } from './api';
import { campusDate, time, dayLabel, maxDate, hasDailyBooking, labels } from './dates';
import './styles.css';

function Mark() { return <span className="mark" aria-hidden="true"><i/><i/><i/><i/></span>; }
function Brand() { return <div className="brand"><Mark/><span>campus<span className="brand-light">slot</span><small>MAKE ROOM FOR YOUR DAY</small></span></div>; }
function Notice({ message, error }) { return message ? <div className={`notice ${error ? 'error' : ''}`} role={error ? 'alert' : 'status'}>{message}</div> : null; }

function Auth({ onLogin }) {
  const [mode, setMode] = useState('login');
  const form = useRef(null);
  const [delivery, setDelivery] = useState(null);
  const [token, setToken] = useState(() => new URLSearchParams(window.location.search).get('verify'));
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  useEffect(() => { if (token) window.history.replaceState({}, '', window.location.pathname); }, [token]);
  useEffect(() => {
    let active = true;
    api('/auth/verification-config').then(config => { if (active) setDelivery(config.delivery); }).catch(() => {});
    return () => { active = false; };
  }, []);
  const verificationHint = delivery === 'console'
    ? 'Development mode: find your verification link in the backend terminal. No email is sent.'
    : delivery === 'smtp' ? 'Check your IIT inbox and spam folder for the verification email.'
    : 'Verify your email before logging in.';
  async function resend() {
    if (!form.current.reportValidity()) return;
    setBusy(true); setError(''); setNotice('');
    const data = Object.fromEntries(new FormData(form.current));
    try {
      await api('/auth/resend-verification', { method: 'POST', body: { email: data.email, password: data.password } });
      setNotice(`If these credentials match an unverified account, a new verification link can be issued once per minute. ${verificationHint}`);
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('');
    const data = Object.fromEntries(new FormData(event.currentTarget));
    try {
      if (token) {
        await api('/auth/verify', { method: 'POST', body: { token } }); setToken(null); setMode('login'); setNotice('Email verified. You can now log in.');
      } else if (mode === 'register') {
        await api('/auth/register', { method: 'POST', body: data }); setMode('login'); setNotice(`Account created. ${verificationHint}`);
      } else {
        await api('/auth/login', { method: 'POST', body: new URLSearchParams({ username: data.email, password: data.password }) });
        onLogin(await api('/auth/me'));
      }
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }
  return <main className="auth-shell">
    <section className="auth-story"><Brand/><div className="story-copy"><span className="eyebrow light">A LITTLE SPACE. A LOT OF POSSIBILITY.</span><h1>Your next great<br/>hour starts here.</h1><p>A quiet room. A place to build. A court to reset.<br/>Find a campus space and make it yours for an hour.</p><div className="story-tags"><span>Study rooms</span><span>Computer labs</span><span>Sports courts</span></div></div><div className="room-art" aria-hidden="true"><div className="window-art"/><div className="lamp-art"/><div className="desk-art"/><div className="chair-art"/><div className="plant-art">✳</div><span className="art-note">A SPACE FOR WHAT’S NEXT ↗</span></div><small className="story-footer">Built for the IIT student community · Independent student project</small></section>
    <section className="auth-form-wrap"><div className="auth-form"><span className="eyebrow">WELCOME TO CAMPUS SLOT</span><h2>{token ? 'One last step.' : mode === 'register' ? 'Find your place.' : 'Welcome back.'}</h2><p className="muted">{token ? 'Verify your IIT email to activate your account.' : mode === 'register' ? 'Create an account using your @iit.ac.lk email.' : 'Log in with your verified IIT email to book a space.'}</p>
      <Notice message={delivery === 'console' ? verificationHint : ''}/><Notice message={notice}/><Notice message={error} error/>
      <form ref={form} onSubmit={submit}>
        {!token && <>
          {mode === 'register' && <label>Full name<input name="name" autoComplete="name" required maxLength="80" placeholder="Your name"/></label>}
          <label>IIT email<input name="email" type="email" autoComplete="username" required maxLength="254" placeholder="yourname@iit.ac.lk"/></label>
          <label>Password<input name="password" type="password" required minLength={mode === 'register' ? 12 : undefined} maxLength="72" autoComplete={mode === 'register' ? 'new-password' : 'current-password'} placeholder={mode === 'register' ? 'At least 12 characters' : 'Enter your password'}/></label>
        </>}
        <button className="button primary wide" disabled={busy}>{busy ? 'Please wait…' : token ? 'Verify my email' : mode === 'register' ? 'Create account →' : 'Log in →'}</button>
      </form>
      {!token && mode === 'login' && <p><button className="text-button" disabled={busy} onClick={resend}>Resend verification link</button><small className="muted"> Enter your account email and password first. Allow one minute between requests.</small></p>}
      {!token && <p className="auth-switch">{mode === 'login' ? 'New here?' : 'Already have an account?'} <button className="text-button" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(''); }}>{mode === 'login' ? 'Create an account' : 'Log in'}</button></p>}
      <div className="auth-rule"><span>◷</span><div><strong>One student. One space. One hour.</strong><p>One session per day, across all facilities.<br/>Your whole room or court is reserved for you.</p></div></div>
    </div></section>
  </main>;
}

function ConfirmBooking({ facility, slot, onClose, onConfirm, busy }) {
  const dialog = useRef(null);
  useEffect(() => { dialog.current.showModal(); }, []);
  return <dialog ref={dialog} onCancel={onClose}><div className="dialog-inner"><span className="eyebrow">YOUR NEXT HOUR</span><h2>Make it yours?</h2><p>{facility.name}</p><div className="booking-detail">{dayLabel(campusDate(slot.startsAt))}<br/><strong>{time(slot.startsAt)} – {time(slot.endsAt)}</strong><small>Sri Lanka time · Entire facility</small></div><p className="muted">This uses your one-hour allowance for this date. You can cancel before the session starts.</p><div className="dialog-actions"><button className="button" disabled={busy} onClick={onClose}>Go back</button><button className="button primary" disabled={busy} onClick={onConfirm}>{busy ? 'Booking…' : 'Confirm booking'}</button></div></div></dialog>;
}

function BookingList({ bookings, onCancel, admin, busy }) {
  if (!bookings.length) return <div className="empty"><span>▤</span><h3>No bookings here yet.</h3><p>Find an available facility to plan your next hour.</p></div>;
  return <div className="booking-list">{bookings.map(b => <article className="booking-row" key={b.id}><div className="date-block"><strong>{new Date(b.startsAt).toLocaleDateString('en-GB', { day: '2-digit', timeZone: 'Asia/Colombo' })}</strong><span>{new Date(b.startsAt).toLocaleDateString('en-GB', { month: 'short', timeZone: 'Asia/Colombo' })}</span></div><div className="booking-info"><h3>{b.facilityName}</h3><p>{time(b.startsAt)} – {time(b.endsAt)} · Entire facility</p>{admin && <small>{b.studentName} · {b.studentEmail}</small>}</div><span className={`badge ${b.status.toLowerCase()}`}>{b.status.toLowerCase()}</span>{b.status === 'CONFIRMED' && new Date(b.startsAt) > new Date() && <button className="text-button danger" disabled={busy} onClick={() => onCancel(b.id)}>Cancel</button>}</article>)}</div>;
}

function AdminPanel({ facilities, date, setDate, run, reload, busy }) {
  const [editing, setEditing] = useState(null);
  const [bookings, setBookings] = useState([]);
  const [loadError, setLoadError] = useState('');
  const [revision, setRevision] = useState(0);
  useEffect(() => { let active = true; setLoadError(''); api(`/admin/bookings?date=${date}`).then(data => { if (active) setBookings(data); }).catch(e => { if (active) setLoadError(e.message); }); return () => { active = false; }; }, [date, revision]);
  function save(event) {
    event.preventDefault(); const form = event.currentTarget; const data = Object.fromEntries(new FormData(form));
    run(async () => { await api(`/admin/facilities${editing ? `/${editing.id}` : ''}`, { method: editing ? 'PUT' : 'POST', body: { ...data, capacity: Number(data.capacity), active: data.active === 'on' } }); setEditing(null); form.reset(); await reload(); }, 'Facility saved.');
  }
  function generate(event) {
    event.preventDefault(); const data = Object.fromEntries(new FormData(event.currentTarget));
    run(async () => { const result = await api(`/admin/facilities/${data.facilityId}/slots`, { method: 'POST', body: { date: data.date, openingHour: Number(data.openingHour), closingHour: Number(data.closingHour) } }); await reload(); return `${result.created} new sessions created.`; });
  }
  return <><div className="section-heading"><div><span className="eyebrow">CAMPUS OPERATIONS</span><h2>Keep the day running.</h2></div></div><div className="admin-grid"><section className="panel"><h3>{editing ? 'Edit facility' : 'Add a facility'}</h3><form onSubmit={save} key={editing?.id ?? 'new'}><label>Name<input name="name" required maxLength="100" defaultValue={editing?.name}/></label><label>Type<select name="type" defaultValue={editing?.type || 'STUDY_ROOM'}>{Object.entries(labels).map(([key,label]) => <option key={key} value={key}>{label}</option>)}</select></label><label>Location<input name="location" required maxLength="160" defaultValue={editing?.location}/></label><label>Capacity<input type="number" name="capacity" required min="1" max="10000" defaultValue={editing?.capacity || 1}/></label>{editing && <label className="check"><input type="checkbox" name="active" defaultChecked={editing.active}/>Available for booking</label>}<button className="button primary" disabled={busy}>Save facility</button>{editing && <button type="button" className="text-button" onClick={() => setEditing(null)}>Cancel edit</button>}</form></section><section className="panel"><h3>Publish one-hour sessions</h3><form onSubmit={generate}><label>Facility<select name="facilityId" required><option value="">Choose a facility</option>{facilities.filter(f => f.active).map(f => <option key={f.id} value={f.id}>{f.name}</option>)}</select></label><label>Date<input type="date" name="date" min={campusDate()} max={maxDate()} required defaultValue={date}/></label><div className="two-cols"><label>Opening hour<input type="number" name="openingHour" min="0" max="23" defaultValue="8" required/></label><label>Closing hour<input type="number" name="closingHour" min="1" max="24" defaultValue="18" required/></label></div><p className="muted small">Hours use the 24-hour clock. Existing sessions are kept. Publish dates up to 30 days ahead.</p><button className="button primary" disabled={busy}>Publish sessions</button></form></section></div><section className="panel"><h3>Facilities</h3>{facilities.map(f => <div className="manage-row" key={f.id}><span><strong>{f.name}</strong><small>{f.location} · {f.active ? 'Active' : 'Inactive'}</small></span><button className="button compact" onClick={() => setEditing(f)}>Edit</button></div>)}</section><section className="panel"><div className="section-heading"><h3>Bookings for a date</h3><input aria-label="Admin booking date" type="date" value={date} onChange={e => setDate(e.target.value)}/></div><Notice message={loadError} error/><BookingList bookings={bookings} admin busy={busy} onCancel={id => { if (window.confirm('Cancel this booking?')) run(async () => { await api(`/admin/bookings/${id}/cancel`, { method: 'POST' }); setRevision(x => x + 1); await reload(); }, 'Booking cancelled.'); }}/></section></>;
}

function Dashboard({ user, onLogout }) {
  const [page, setPage] = useState('explore');
  const [facilities, setFacilities] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [slots, setSlots] = useState([]);
  const [facilityId, setFacilityId] = useState(null);
  const [filter, setFilter] = useState('ALL');
  const [date, setDate] = useState(campusDate());
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [slotLoading, setSlotLoading] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(null);
  const [revision, setRevision] = useState(0);
  const admin = user.role === 'ADMIN';
  const facility = facilities.find(f => f.id === facilityId);
  const visible = facilities.filter(f => filter === 'ALL' || f.type === filter);
  const quotaUsed = hasDailyBooking(bookings, date);
  async function reload() {
    const [fs, bs] = await Promise.all([api('/facilities'), api('/bookings/mine')]);
    setFacilities(fs); setBookings(bs); setFacilityId(id => fs.some(f => f.id === id) ? id : fs[0]?.id ?? null); setRevision(x => x + 1);
  }
  useEffect(() => {
    let active = true;
    const refresh = () => { if (active) reload().catch(e => { if (active) { setError(e.message); if (e.status === 401) onLogout(); } }).finally(() => { if (active) setLoading(false); }); };
    refresh(); const timer = setInterval(refresh, 60000);
    return () => { active = false; clearInterval(timer); };
  }, []);
  useEffect(() => {
    if (!facilityId) { setSlots([]); return; }
    let active = true; setSlotLoading(true);
    api(`/facilities/${facilityId}/slots?date=${date}`).then(data => { if (active) setSlots(data); }).catch(e => { if (active) setError(e.message); }).finally(() => { if (active) setSlotLoading(false); });
    return () => { active = false; };
  }, [facilityId, date, revision]);
  async function run(action, message) {
    if (busy) return; setBusy(true); setError(''); setNotice('');
    try { const result = await action(); setNotice(typeof result === 'string' ? result : message || 'Saved.'); }
    catch (e) { setError(e.message); if (e.status === 401) onLogout(); else reload().catch(() => {}); }
    finally { setBusy(false); }
  }
  const upcoming = bookings.filter(b => b.status === 'CONFIRMED' && new Date(b.endsAt) > new Date());
  return <div className="app-shell"><aside className="sidebar"><Brand/><div className="nav-caption">YOUR CAMPUS</div><nav><button className={page === 'explore' ? 'active' : ''} onClick={() => setPage('explore')}><span>▦</span>Explore spaces</button><button className={page === 'bookings' ? 'active' : ''} onClick={() => setPage('bookings')}><span>▤</span>My bookings {upcoming.length > 0 && <small>{upcoming.length}</small>}</button>{admin && <button className={page === 'admin' ? 'active' : ''} onClick={() => setPage('admin')}><span>⚙</span>Administration</button>}</nav><div className="sidebar-note"><span>◷</span><h3>A fair share of space.</h3><p>One hour a day.<br/>A whole facility for you.</p></div><div className="sidebar-bottom">Independent IIT student project<br/><span>All times in Sri Lanka · UTC+05:30</span></div></aside><div className="workspace"><header className="topbar"><span>Campus / <strong>{page === 'admin' ? 'Administration' : page === 'bookings' ? 'My bookings' : 'Explore spaces'}</strong></span><div className="user-menu"><span className="avatar">{user.name.slice(0,1).toUpperCase()}</span><span>{user.name}<small>{admin ? 'Administrator' : 'IIT student'}</small></span><button className="text-button" disabled={busy} onClick={() => run(async () => { await api('/auth/logout', { method: 'POST' }); onLogout(); })}>Log out</button></div></header><main className="content"><Notice message={notice}/><Notice message={error} error/>
    {page === 'explore' && <><section className="welcome"><div><span className="eyebrow">SPACE TO DO YOUR THING</span><h1>Make room for<br/><em>your next idea.</em></h1><p>Find a space for focus, collaboration or a little competition.<br/> Choose a facility. Pick an hour. You’re all set.</p><span className="welcome-pill"><i/>Instant booking · No approval needed</span></div><div className="welcome-art" aria-hidden="true"><div className="outline-room"/><div className="mini-table"/><div className="mini-chair"/><span>YOUR CAMPUS,<br/><b>YOUR HOUR.</b></span></div></section><div className="stat-grid"><div><span>Spaces to choose from</span><strong>{facilities.filter(f => f.active).length.toString().padStart(2,'0')}</strong></div><div><span>Your upcoming sessions</span><strong>{upcoming.length.toString().padStart(2,'0')}</strong></div><div><span>Your daily allowance</span><strong>01 <small>hour</small></strong></div></div><section className="spaces-section"><div className="section-heading"><div><span className="eyebrow">FIND YOUR PLACE</span><h2>Good spaces. Great possibilities.</h2></div><label className="date-filter">Booking date<input type="date" value={date} min={campusDate()} max={maxDate()} onChange={e => setDate(e.target.value || campusDate())}/></label></div><div className="filters" role="group" aria-label="Facility type">{[['ALL','All spaces'],...Object.entries(labels)].map(([key,label]) => <button key={key} className={filter === key ? 'selected' : ''} onClick={() => setFilter(key)}>{label}</button>)}</div>{loading ? <p role="status">Loading campus spaces…</p> : !visible.length ? <div className="empty"><h3>No facilities published yet.</h3><p>Check back when your administrator adds facilities.</p></div> : <div className="facility-grid">{visible.map((f, index) => <button className={`facility-card ${facilityId === f.id ? 'chosen' : ''}`} key={f.id} onClick={() => setFacilityId(f.id)} aria-pressed={facilityId === f.id}><div className={`facility-art ${f.type.toLowerCase()}`} aria-hidden="true"><span className="art-number">{String(index + 1).padStart(2,'0')}</span><div className="space-icon">{f.type === 'STUDY_ROOM' ? '▥' : f.type === 'COMPUTER_LAB' ? '▣' : '◎'}</div><span className="facility-tag">{f.type === 'STUDY_ROOM' ? 'FOCUS & COLLABORATE' : f.type === 'COMPUTER_LAB' ? 'BUILD SOMETHING' : 'MOVE & RECHARGE'}</span></div><div className="facility-copy"><span className="eyebrow">{labels[f.type]}</span><h3>{f.name}<span>↗</span></h3><p>{f.location}</p><div className="facility-meta"><span>Up to {f.capacity} people</span><span>{f.active ? 'Whole facility' : 'Inactive'}</span></div></div></button>)}</div>}</section>{facility && <section className="panel slot-panel"><div className="section-heading"><div><span className="eyebrow">{dayLabel(date).toUpperCase()} · SRI LANKA TIME</span><h2>Your hour at {facility.name}</h2></div><span className="badge confirmed">60 minutes</span></div>{!admin && quotaUsed && <Notice message="You already have a session for this date. Cancel it before it starts if you want to choose a different space."/>}{slotLoading ? <p role="status">Checking availability…</p> : !slots.length ? <div className="empty"><h3>No sessions published for this date.</h3><p>Choose another date or check back later.</p></div> : <div className="slot-grid">{slots.map(s => <div className="slot-item" key={s.id}><button className={`slot ${s.state.toLowerCase()}`} disabled={busy || (admin ? true : s.state !== 'AVAILABLE' || quotaUsed)} onClick={() => setPending(s)}><strong>{time(s.startsAt)}</strong><span>to {time(s.endsAt)}</span><small>{s.state.toLowerCase()}</small></button>{admin && ['AVAILABLE','BLOCKED'].includes(s.state) && <button className="text-button small" disabled={busy} onClick={() => run(async () => { await api(`/admin/slots/${s.id}`, { method: 'PATCH', body: { blocked: s.state === 'AVAILABLE' } }); await reload(); }, 'Session updated.')}>{s.state === 'BLOCKED' ? 'Unblock' : 'Block'}</button>}</div>)}</div>}<p className="muted small">The entire facility is reserved. Sessions finish automatically at their scheduled end.</p></section>}</>}
    {page === 'bookings' && <><div className="section-heading"><div><span className="eyebrow">YOUR TIME, WELL SPENT</span><h1>My bookings.</h1><p className="muted">Upcoming sessions and the hours you’ve made yours.</p></div></div><section className="panel"><BookingList bookings={bookings} busy={busy} onCancel={id => { if (window.confirm('Cancel this booking? Your daily allowance will be available again.')) run(async () => { await api(`/bookings/${id}/cancel`, { method: 'POST' }); await reload(); }, 'Booking cancelled.'); }}/></section></>}
    {page === 'admin' && admin && <AdminPanel facilities={facilities} date={date} setDate={setDate} run={run} reload={reload} busy={busy}/>}
    <footer className="page-footer"><span>campusslot · A little space goes a long way.</span><span>Whole facilities. One-hour sessions. Fair access.</span></footer>
    </main></div>{pending && facility && <ConfirmBooking facility={facility} slot={pending} busy={busy} onClose={() => { if (!busy) setPending(null); }} onConfirm={() => run(async () => { await api('/bookings', { method: 'POST', body: { slotId: pending.id } }); setPending(null); await reload(); }, 'Your space is booked. Find it in My bookings.')}/>}</div>;
}

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => { api('/auth/me').then(setUser).catch(e => { if (e.status !== 401) setError('Cannot reach Campus Slot. Check that the backend is running, then refresh.'); }).finally(() => setLoading(false)); }, []);
  if (loading) return <div className="startup" role="status"><Brand/><p>Finding your space…</p></div>;
  if (error) return <div className="startup"><Brand/><Notice message={error} error/><button className="button primary" onClick={() => window.location.reload()}>Try again</button></div>;
  return user ? <Dashboard user={user} onLogout={() => setUser(null)}/> : <Auth onLogin={setUser}/>;
}

createRoot(document.getElementById('root')).render(<App/>);
