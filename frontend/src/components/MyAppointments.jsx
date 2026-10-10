import React, { useState, useEffect } from 'react';
import { api } from '../services/api';

export default function MyAppointments({ onOpenBookingForDoctor }) {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Reschedule state
  const [reschedulingAppt, setReschedulingAppt] = useState(null);
  const [doctorSlots, setDoctorSlots] = useState([]);
  const [selectedNewSlotId, setSelectedNewSlotId] = useState('');
  const [rescheduling, setRescheduling] = useState(false);
  const [rescheduleError, setRescheduleError] = useState(null);

  // Cancellation state
  const [cancellingAppt, setCancellingAppt] = useState(null);
  const [cancellationReason, setCancellationReason] = useState('');
  const [cancelling, setCancelling] = useState(false);
  const [cancelError, setCancelError] = useState(null);

  useEffect(() => {
    loadAppointments();
  }, []);

  async function loadAppointments() {
    try {
      setLoading(true);
      setError(null);
      const data = await api.getMyAppointments();
      setAppointments(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function formatDateTime(isoString) {
    if (!isoString) return '—';
    const d = new Date(isoString);
    return d.toLocaleString('en-GB', {
      timeZone: 'Africa/Kigali',
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }) + ' CAT';
  }

  function getLeadHours(startTimeStr) {
    const start = new Date(startTimeStr).getTime();
    const now = Date.now();
    return (start - now) / (3600 * 1000);
  }

  // Handle open reschedule modal
  async function openRescheduleModal(appt) {
    setReschedulingAppt(appt);
    setRescheduleError(null);
    setSelectedNewSlotId('');
    try {
      const slots = await api.getDoctorSlots(appt.doctorId);
      // Filter out the slot they currently hold
      const available = slots.filter(s => s.id !== appt.slotId);
      setDoctorSlots(available);
      if (available.length > 0) setSelectedNewSlotId(available[0].id);
    } catch (err) {
      setRescheduleError('Failed to fetch alternate doctor slots: ' + err.message);
    }
  }

  async function handleConfirmReschedule() {
    if (!selectedNewSlotId) return;
    try {
      setRescheduling(true);
      setRescheduleError(null);
      await api.rescheduleAppointment(reschedulingAppt.id, selectedNewSlotId);
      setReschedulingAppt(null);
      await loadAppointments();
    } catch (err) {
      setRescheduleError(err.message);
    } finally {
      setRescheduling(false);
    }
  }

  async function handleConfirmCancellation() {
    try {
      setCancelling(true);
      setCancelError(null);
      await api.cancelAppointment(cancellingAppt.id, cancellationReason || 'Patient requested cancellation');
      setCancellingAppt(null);
      setCancellationReason('');
      await loadAppointments();
    } catch (err) {
      setCancelError(err.message);
    } finally {
      setCancelling(false);
    }
  }

  const filteredAppointments = appointments.filter(a => {
    if (statusFilter === 'ALL') return true;
    return a.status === statusFilter;
  });

  return (
    <div>
      {/* Title & Filter Tabs */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.6rem', color: '#f8fafc', marginBottom: '4px' }}>My Appointments</h2>
          <p style={{ color: '#94a3b8', fontSize: '0.9rem' }}>
            Track consultation schedule, reschedule dates, or cancel bookings in accordance with hospital policy.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          {['ALL', 'CONFIRMED', 'COMPLETED', 'CANCELLED'].map(tab => (
            <button
              key={tab}
              onClick={() => setStatusFilter(tab)}
              style={{
                padding: '6px 14px',
                borderRadius: '8px',
                fontSize: '0.8rem',
                fontWeight: 600,
                background: statusFilter === tab ? 'var(--primary-teal)' : 'rgba(255,255,255,0.05)',
                color: statusFilter === tab ? '#ffffff' : '#94a3b8',
                border: statusFilter === tab ? 'none' : '1px solid rgba(255,255,255,0.08)',
              }}
            >
              {tab}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: '#94a3b8' }}>
          <div className="pulse-indicator" style={{ width: '12px', height: '12px', marginBottom: '10px' }}></div>
          <div>Loading your appointment history...</div>
        </div>
      ) : error ? (
        <div className="glass-panel" style={{ padding: '24px', color: '#f87171' }}>
          Failed to load appointments: {error}
        </div>
      ) : filteredAppointments.length === 0 ? (
        <div className="glass-panel" style={{ textAlign: 'center', padding: '60px 24px' }}>
          <div style={{ fontSize: '2.5rem', marginBottom: '12px' }}>📋</div>
          <h3 style={{ color: '#f8fafc', marginBottom: '6px' }}>No Appointments Found</h3>
          <p style={{ color: '#94a3b8', fontSize: '0.9rem', marginBottom: '20px' }}>
            You currently have no consultations matching the filter.
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {filteredAppointments.map(appt => {
            const leadHours = getLeadHours(appt.startTime);
            const isWithinTwoHours = leadHours < 2;
            const isConfirmed = appt.status === 'CONFIRMED';

            return (
              <div
                key={appt.id}
                className="glass-panel"
                style={{
                  padding: '24px',
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
                  gap: '20px',
                  alignItems: 'center',
                }}
              >
                {/* Reference & Doctor Info */}
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                    <span style={{
                      fontFamily: 'monospace',
                      fontWeight: 800,
                      fontSize: '1rem',
                      color: '#f8fafc',
                      background: 'rgba(0,0,0,0.3)',
                      padding: '3px 8px',
                      borderRadius: '6px',
                    }}>
                      {appt.referenceNo}
                    </span>
                    <span className={`badge badge-${appt.status.toLowerCase()}`}>
                      {appt.status}
                    </span>
                  </div>
                  <h3 style={{ fontSize: '1.15rem', color: '#f8fafc', marginBottom: '4px' }}>
                    {appt.doctorName}
                  </h3>
                  <div style={{ fontSize: '0.85rem', color: '#14b8a6' }}>
                    {appt.departmentName}
                  </div>
                </div>

                {/* Timing & Reason */}
                <div>
                  <div style={{ fontSize: '0.8rem', color: '#94a3b8', marginBottom: '4px' }}>Appointment Time:</div>
                  <div style={{ fontSize: '0.95rem', fontWeight: 600, color: '#f8fafc', marginBottom: '8px' }}>
                    📅 {formatDateTime(appt.startTime)}
                  </div>
                  <div style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
                    Chief Complaint: <span style={{ color: '#cbd5e1' }}>{appt.reason}</span>
                  </div>
                  {appt.cancellationReason && (
                    <div style={{ fontSize: '0.8rem', color: '#f87171', marginTop: '4px' }}>
                      Cancellation Reason: {appt.cancellationReason}
                    </div>
                  )}
                </div>

                {/* Lead-time Policy & Actions */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', alignItems: 'flex-start' }}>
                  {isConfirmed && (
                    <>
                      <div style={{ fontSize: '0.75rem', color: isWithinTwoHours ? '#f59e0b' : '#10b981', fontWeight: 600 }}>
                        {isWithinTwoHours ? (
                          <span>⚠️ Starts in &lt; 2h (Online Cancel/Reschedule Locked)</span>
                        ) : (
                          <span>✓ Online Reschedule / Cancel Allowed (&gt; 2h ahead)</span>
                        )}
                      </div>

                      <div style={{ display: 'flex', gap: '10px' }}>
                        <button
                          onClick={() => openRescheduleModal(appt)}
                          disabled={isWithinTwoHours}
                          className="btn-secondary"
                          style={{
                            fontSize: '0.85rem',
                            padding: '6px 14px',
                            opacity: isWithinTwoHours ? 0.5 : 1,
                            cursor: isWithinTwoHours ? 'not-allowed' : 'pointer',
                          }}
                          title={isWithinTwoHours ? 'Locked within 2 hours of appointment start time' : 'Atomic Reschedule to a new slot'}
                        >
                          🔄 Reschedule
                        </button>

                        <button
                          onClick={() => { setCancellingAppt(appt); setCancelError(null); }}
                          disabled={isWithinTwoHours}
                          className="btn-danger"
                          style={{
                            fontSize: '0.85rem',
                            padding: '6px 14px',
                            opacity: isWithinTwoHours ? 0.5 : 1,
                            cursor: isWithinTwoHours ? 'not-allowed' : 'pointer',
                          }}
                          title={isWithinTwoHours ? 'Locked within 2 hours of appointment start time' : 'Cancel appointment'}
                        >
                          Cancel Booking
                        </button>
                      </div>
                    </>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Atomic Reschedule Modal */}
      {reschedulingAppt && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={{ color: '#f8fafc', marginBottom: '8px' }}>
              Reschedule Appointment {reschedulingAppt.referenceNo}
            </h3>
            <p style={{ color: '#94a3b8', fontSize: '0.85rem', marginBottom: '18px' }}>
              Select a new time slot with {reschedulingAppt.doctorName}. The current slot will be freed atomically (FR-APPT-06).
            </p>

            {rescheduleError && (
              <div style={{ color: '#f87171', background: 'rgba(239, 68, 68, 0.1)', padding: '10px', borderRadius: '8px', marginBottom: '14px', fontSize: '0.85rem' }}>
                {rescheduleError}
              </div>
            )}

            <div className="form-group">
              <label className="form-label">Available Alternative Slots:</label>
              {doctorSlots.length === 0 ? (
                <div style={{ color: '#fbbf24', fontSize: '0.85rem' }}>No alternate open slots found.</div>
              ) : (
                <select
                  value={selectedNewSlotId}
                  onChange={(e) => setSelectedNewSlotId(e.target.value)}
                  className="form-select"
                >
                  {doctorSlots.map(s => (
                    <option key={s.id} value={s.id}>
                      {formatDateTime(s.startTime)}
                    </option>
                  ))}
                </select>
              )}
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', marginTop: '20px' }}>
              <button
                onClick={() => setReschedulingAppt(null)}
                className="btn-secondary"
                disabled={rescheduling}
              >
                Close
              </button>
              <button
                onClick={handleConfirmReschedule}
                className="btn-primary"
                disabled={rescheduling || !selectedNewSlotId}
              >
                {rescheduling ? 'Rescheduling...' : 'Confirm New Slot'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Cancellation Confirmation Modal */}
      {cancellingAppt && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={{ color: '#f87171', marginBottom: '8px' }}>
              Cancel Appointment {cancellingAppt.referenceNo}?
            </h3>
            <p style={{ color: '#94a3b8', fontSize: '0.85rem', marginBottom: '16px' }}>
              This will release your booked slot back to the outpatient schedule immediately.
            </p>

            {cancelError && (
              <div style={{ color: '#f87171', background: 'rgba(239, 68, 68, 0.1)', padding: '10px', borderRadius: '8px', marginBottom: '14px', fontSize: '0.85rem' }}>
                {cancelError}
              </div>
            )}

            <div className="form-group">
              <label className="form-label">Reason for Cancellation:</label>
              <input
                type="text"
                placeholder="e.g. Schedule conflict, symptoms resolved..."
                value={cancellationReason}
                onChange={(e) => setCancellationReason(e.target.value)}
                className="form-input"
              />
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', marginTop: '20px' }}>
              <button
                onClick={() => setCancellingAppt(null)}
                className="btn-secondary"
                disabled={cancelling}
              >
                Keep Appointment
              </button>
              <button
                onClick={handleConfirmCancellation}
                className="btn-danger"
                disabled={cancelling}
              >
                {cancelling ? 'Cancelling...' : 'Confirm Cancellation'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
