import React, { useState, useEffect } from 'react';
import { api } from '../services/api';

const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

export default function DoctorConsole({ currentUser }) {
  const [todayAppts, setTodayAppts] = useState([]);
  const [loadingAppts, setLoadingAppts] = useState(true);
  const [activeSubTab, setActiveSubTab] = useState('queue'); // 'queue', 'hours', 'leave'
  const [actionNotes, setActionNotes] = useState({});

  // Leave Form
  const [leaveStart, setLeaveStart] = useState('');
  const [leaveEnd, setLeaveEnd] = useState('');
  const [leaveReason, setLeaveReason] = useState('');
  const [leaveSuccess, setLeaveSuccess] = useState(null);
  const [leaveError, setLeaveError] = useState(null);
  const [submittingLeave, setSubmittingLeave] = useState(false);

  // Hours Form
  const [workingHours, setWorkingHours] = useState([
    { dayOfWeek: 1, startTime: '08:00', endTime: '15:00' },
    { dayOfWeek: 2, startTime: '08:00', endTime: '15:00' },
    { dayOfWeek: 3, startTime: '08:00', endTime: '15:00' },
    { dayOfWeek: 4, startTime: '08:00', endTime: '15:00' },
    { dayOfWeek: 5, startTime: '08:00', endTime: '15:00' },
  ]);
  const [hoursSuccess, setHoursSuccess] = useState(null);
  const [hoursError, setHoursError] = useState(null);
  const [submittingHours, setSubmittingHours] = useState(false);

  useEffect(() => {
    loadTodayAppointments();
  }, []);

  async function loadTodayAppointments() {
    try {
      setLoadingAppts(true);
      const data = await api.getDoctorTodayAppointments();
      setTodayAppts(data || []);
    } catch (err) {
      console.error('Failed to load today appointments', err);
    } finally {
      setLoadingAppts(false);
    }
  }

  async function handleUpdateStatus(apptId, newStatus) {
    try {
      const notes = actionNotes[apptId] || '';
      await api.updateAppointmentStatus(apptId, newStatus, notes);
      await loadTodayAppointments();
    } catch (err) {
      alert('Error updating status: ' + err.message);
    }
  }

  async function handleSaveHours(e) {
    e.preventDefault();
    try {
      setSubmittingHours(true);
      setHoursError(null);
      setHoursSuccess(null);
      // format HH:mm:00
      const payload = workingHours.map(h => ({
        dayOfWeek: h.dayOfWeek,
        startTime: h.startTime.length === 5 ? h.startTime + ':00' : h.startTime,
        endTime: h.endTime.length === 5 ? h.endTime + ':00' : h.endTime,
      }));
      await api.updateWorkingHours(payload);
      setHoursSuccess('Recurring working hours updated and 14-day schedule regenerated successfully!');
    } catch (err) {
      setHoursError(err.message);
    } finally {
      setSubmittingHours(false);
    }
  }

  async function handleAddLeave(e) {
    e.preventDefault();
    try {
      setSubmittingLeave(true);
      setLeaveError(null);
      setLeaveSuccess(null);
      await api.addLeaveBlock({
        startDate: leaveStart,
        endDate: leaveEnd,
        reason: leaveReason,
      });
      setLeaveSuccess(`Leave from ${leaveStart} to ${leaveEnd} registered. Unbooked slots cleared.`);
      setLeaveStart('');
      setLeaveEnd('');
      setLeaveReason('');
    } catch (err) {
      setLeaveError(err.message);
    } finally {
      setSubmittingLeave(false);
    }
  }

  function formatTime(isoStr) {
    const d = new Date(isoStr);
    return d.toLocaleTimeString('en-GB', {
      timeZone: 'Africa/Kigali',
      hour: '2-digit',
      minute: '2-digit',
    }) + ' CAT';
  }

  return (
    <div>
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ fontSize: '1.6rem', color: '#f8fafc', marginBottom: '4px' }}>
          Doctor Console · {currentUser?.name}
        </h2>
        <p style={{ color: '#94a3b8', fontSize: '0.9rem' }}>
          Manage your daily patient queue, customize outpatient shift hours, and record official leave dates.
        </p>
      </div>

      {/* Sub Tabs */}
      <div style={{ display: 'flex', gap: '10px', marginBottom: '24px' }}>
        <button
          onClick={() => setActiveSubTab('queue')}
          style={{
            padding: '8px 18px',
            borderRadius: '8px',
            fontSize: '0.9rem',
            fontWeight: 600,
            background: activeSubTab === 'queue' ? 'var(--primary-teal)' : 'rgba(255,255,255,0.05)',
            color: activeSubTab === 'queue' ? '#ffffff' : '#94a3b8',
          }}
        >
          📋 Today's Patient Queue ({todayAppts.length})
        </button>
        <button
          onClick={() => setActiveSubTab('hours')}
          style={{
            padding: '8px 18px',
            borderRadius: '8px',
            fontSize: '0.9rem',
            fontWeight: 600,
            background: activeSubTab === 'hours' ? 'var(--primary-teal)' : 'rgba(255,255,255,0.05)',
            color: activeSubTab === 'hours' ? '#ffffff' : '#94a3b8',
          }}
        >
          ⏰ Recurring Working Hours
        </button>
        <button
          onClick={() => setActiveSubTab('leave')}
          style={{
            padding: '8px 18px',
            borderRadius: '8px',
            fontSize: '0.9rem',
            fontWeight: 600,
            background: activeSubTab === 'leave' ? 'var(--primary-teal)' : 'rgba(255,255,255,0.05)',
            color: activeSubTab === 'leave' ? '#ffffff' : '#94a3b8',
          }}
        >
          🏖️ Leave & Availability Blocker
        </button>
      </div>

      {/* Queue View */}
      {activeSubTab === 'queue' && (
        <div>
          {loadingAppts ? (
            <div style={{ textAlign: 'center', padding: '60px 0', color: '#94a3b8' }}>
              <div className="pulse-indicator" style={{ width: '12px', height: '12px', marginBottom: '10px' }}></div>
              <div>Loading today's patient queue...</div>
            </div>
          ) : todayAppts.length === 0 ? (
            <div className="glass-panel" style={{ textAlign: 'center', padding: '48px 24px' }}>
              <div style={{ fontSize: '2rem', marginBottom: '10px' }}>☕</div>
              <h3 style={{ color: '#f8fafc', marginBottom: '6px' }}>No Appointments Today</h3>
              <p style={{ color: '#94a3b8', fontSize: '0.9rem' }}>No patients currently scheduled for today's clinic.</p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {todayAppts.map(a => (
                <div key={a.id} className="glass-panel" style={{ padding: '20px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '14px', marginBottom: '14px' }}>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                        <span style={{ fontFamily: 'monospace', fontWeight: 800, color: '#f8fafc', background: 'rgba(0,0,0,0.3)', padding: '2px 8px', borderRadius: '4px' }}>
                          {a.referenceNo}
                        </span>
                        <span className={`badge badge-${a.status.toLowerCase()}`}>{a.status}</span>
                      </div>
                      <h3 style={{ fontSize: '1.2rem', color: '#f8fafc' }}>{a.patientName}</h3>
                      <div style={{ fontSize: '0.85rem', color: '#94a3b8' }}>Phone: {a.patientPhone}</div>
                    </div>

                    <div style={{ textAlign: 'right' }}>
                      <div style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Slot Time:</div>
                      <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#2dd4bf' }}>
                        {formatTime(a.startTime)}
                      </div>
                    </div>
                  </div>

                  <div style={{ background: 'rgba(0,0,0,0.2)', padding: '10px 14px', borderRadius: '8px', marginBottom: '14px', fontSize: '0.85rem' }}>
                    <span style={{ color: '#94a3b8' }}>Reason for Visit: </span>
                    <span style={{ color: '#e2e8f0' }}>{a.reason}</span>
                  </div>

                  {a.status === 'CONFIRMED' && (
                    <div style={{ display: 'flex', gap: '12px', alignItems: 'center', flexWrap: 'wrap' }}>
                      <input
                        type="text"
                        placeholder="Add clinical consultation summary notes..."
                        value={actionNotes[a.id] || ''}
                        onChange={(e) => setActionNotes({ ...actionNotes, [a.id]: e.target.value })}
                        className="form-input"
                        style={{ flex: '1 1 240px', padding: '6px 12px', fontSize: '0.85rem' }}
                      />
                      <button
                        onClick={() => handleUpdateStatus(a.id, 'COMPLETED')}
                        className="btn-primary"
                        style={{ fontSize: '0.85rem', padding: '6px 14px' }}
                      >
                        ✓ Mark Completed
                      </button>
                      <button
                        onClick={() => handleUpdateStatus(a.id, 'NO_SHOW')}
                        style={{
                          background: 'rgba(245, 158, 11, 0.15)',
                          border: '1px solid rgba(245, 158, 11, 0.3)',
                          color: '#fbbf24',
                          padding: '6px 14px',
                          borderRadius: '8px',
                          fontSize: '0.85rem',
                          fontWeight: 600,
                        }}
                      >
                        ✕ Mark No-Show
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Working Hours Editor */}
      {activeSubTab === 'hours' && (
        <div className="glass-panel" style={{ padding: '28px', maxWidth: '640px' }}>
          <h3 style={{ color: '#f8fafc', marginBottom: '6px' }}>Configure Weekly Outpatient Clinic Hours</h3>
          <p style={{ color: '#94a3b8', fontSize: '0.85rem', marginBottom: '20px' }}>
            Working hours must fall strictly within your department opening hours (07:00 - 17:00 CAT).
          </p>

          {hoursSuccess && (
            <div style={{ background: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.3)', color: '#34d399', padding: '12px', borderRadius: '8px', marginBottom: '16px', fontSize: '0.85rem' }}>
              {hoursSuccess}
            </div>
          )}
          {hoursError && (
            <div style={{ background: 'rgba(239, 68, 68, 0.15)', border: '1px solid rgba(239, 68, 68, 0.3)', color: '#f87171', padding: '12px', borderRadius: '8px', marginBottom: '16px', fontSize: '0.85rem' }}>
              {hoursError}
            </div>
          )}

          <form onSubmit={handleSaveHours}>
            {workingHours.map((h, idx) => (
              <div key={h.dayOfWeek} style={{ display: 'grid', gridTemplateColumns: '120px 1fr 1fr', gap: '14px', alignItems: 'center', marginBottom: '12px' }}>
                <span style={{ fontWeight: 600, color: '#f8fafc', fontSize: '0.9rem' }}>
                  {DAYS[h.dayOfWeek - 1]}
                </span>
                <input
                  type="time"
                  value={h.startTime}
                  onChange={(e) => {
                    const updated = [...workingHours];
                    updated[idx].startTime = e.target.value;
                    setWorkingHours(updated);
                  }}
                  className="form-input"
                  style={{ padding: '6px 10px' }}
                />
                <input
                  type="time"
                  value={h.endTime}
                  onChange={(e) => {
                    const updated = [...workingHours];
                    updated[idx].endTime = e.target.value;
                    setWorkingHours(updated);
                  }}
                  className="form-input"
                  style={{ padding: '6px 10px' }}
                />
              </div>
            ))}

            <button type="submit" className="btn-primary" disabled={submittingHours} style={{ marginTop: '16px' }}>
              {submittingHours ? 'Updating & Regenerating Slots...' : 'Save Weekly Schedule'}
            </button>
          </form>
        </div>
      )}

      {/* Leave Blocker View */}
      {activeSubTab === 'leave' && (
        <div className="glass-panel" style={{ padding: '28px', maxWidth: '640px' }}>
          <h3 style={{ color: '#f8fafc', marginBottom: '6px' }}>Register Official Leave / Off-Duty Dates</h3>
          <p style={{ color: '#94a3b8', fontSize: '0.85rem', marginBottom: '20px' }}>
            System will reject leave if any Confirmed patient appointments overlap the dates (FR-SCHED-04).
          </p>

          {leaveSuccess && (
            <div style={{ background: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.3)', color: '#34d399', padding: '12px', borderRadius: '8px', marginBottom: '16px', fontSize: '0.85rem' }}>
              {leaveSuccess}
            </div>
          )}
          {leaveError && (
            <div style={{ background: 'rgba(239, 68, 68, 0.15)', border: '1px solid rgba(239, 68, 68, 0.3)', color: '#f87171', padding: '12px', borderRadius: '8px', marginBottom: '16px', fontSize: '0.85rem' }}>
              {leaveError}
            </div>
          )}

          <form onSubmit={handleAddLeave}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '14px' }}>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">* Start Date:</label>
                <input
                  type="date"
                  required
                  value={leaveStart}
                  onChange={(e) => setLeaveStart(e.target.value)}
                  className="form-input"
                />
              </div>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">* End Date:</label>
                <input
                  type="date"
                  required
                  value={leaveEnd}
                  onChange={(e) => setLeaveEnd(e.target.value)}
                  className="form-input"
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">* Reason for Leave / Block:</label>
              <input
                type="text"
                required
                placeholder="e.g. Annual Leave, Continuing Medical Education (CME), Mission..."
                value={leaveReason}
                onChange={(e) => setLeaveReason(e.target.value)}
                className="form-input"
              />
            </div>

            <button type="submit" className="btn-primary" disabled={submittingLeave}>
              {submittingLeave ? 'Verifying Bookings & Blocking...' : 'Register Leave Block'}
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
