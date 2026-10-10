import React, { useState, useEffect } from 'react';
import { api } from '../services/api';

export default function BookingModal({ doctor, currentUser, onClose, onBookingSuccess }) {
  const [slots, setSlots] = useState([]);
  const [selectedSlotId, setSelectedSlotId] = useState('');
  const [reason, setReason] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('MTN_MOMO');
  const [paymentMode, setPaymentMode] = useState('SUCCESS'); // SUCCESS, DECLINE, TIMEOUT
  const [overrideReason, setOverrideReason] = useState('');
  const [loadingSlots, setLoadingSlots] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [successReceipt, setSuccessReceipt] = useState(null);

  const isStaff = currentUser?.role === 'RECEPTIONIST' || currentUser?.role === 'ADMIN';

  useEffect(() => {
    loadSlots();
  }, [doctor]);

  async function loadSlots() {
    try {
      setLoadingSlots(true);
      setError(null);
      const data = await api.getDoctorSlots(doctor.id);
      setSlots(data || []);
      if (data && data.length > 0) {
        setSelectedSlotId(data[0].id);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoadingSlots(false);
    }
  }

  function formatSlotDateTime(isoString) {
    const d = new Date(isoString);
    return d.toLocaleString('en-GB', {
      timeZone: 'Africa/Kigali',
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  function isSlotUnderTwoHours(startTimeStr) {
    const start = new Date(startTimeStr).getTime();
    const now = Date.now();
    return (start - now) < (2 * 3600 * 1000);
  }

  const selectedSlot = slots.find(s => s.id === selectedSlotId);
  const isUrgentSlot = selectedSlot ? isSlotUnderTwoHours(selectedSlot.startTime) : false;

  async function handleBook(e) {
    e.preventDefault();
    if (!selectedSlotId) {
      setError('Please select an available appointment slot.');
      return;
    }
    if (!reason.trim()) {
      setError('Reason for visit is required.');
      return;
    }
    if (isUrgentSlot && !isStaff) {
      setError('Online bookings must be made at least 2 hours in advance. For emergency/walk-ins, please visit hospital reception.');
      return;
    }
    if (isUrgentSlot && isStaff && !overrideReason.trim()) {
      setError('Staff override for urgent slot requires an explicit reason.');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      const result = await api.bookAppointment({
        slotId: selectedSlotId,
        reason: reason.trim(),
        paymentMethod: paymentMethod,
        paymentMode: paymentMode,
        overrideReason: isStaff && isUrgentSlot ? overrideReason.trim() : null,
      });
      setSuccessReceipt(result);
      if (onBookingSuccess) onBookingSuccess(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-content" style={{ maxHeight: '90vh', overflowY: 'auto' }}>
        {/* Modal Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '20px' }}>
          <div>
            <span className="badge badge-open" style={{ marginBottom: '6px' }}>Book Outpatient Visit</span>
            <h2 style={{ fontSize: '1.4rem', color: '#f8fafc' }}>{doctor.fullName}</h2>
            <div style={{ fontSize: '0.85rem', color: '#14b8a6' }}>
              {doctor.departmentName} · Consultation Fee: {doctor.consultationFeeRwf.toLocaleString()} RWF
            </div>
          </div>
          <button
            onClick={onClose}
            style={{ background: 'transparent', color: '#94a3b8', fontSize: '1.4rem', padding: '4px' }}
          >
            ✕
          </button>
        </div>

        {/* Success Confirmation Receipt State */}
        {successReceipt ? (
          <div>
            <div style={{
              background: 'rgba(16, 185, 129, 0.1)',
              border: '1px solid rgba(16, 185, 129, 0.3)',
              borderRadius: '16px',
              padding: '24px',
              textAlign: 'center',
              marginBottom: '20px',
            }}>
              <div style={{ fontSize: '2.5rem', marginBottom: '10px' }}>✅</div>
              <h3 style={{ color: '#34d399', fontSize: '1.3rem', marginBottom: '6px' }}>
                Appointment Confirmed!
              </h3>
              <div style={{
                fontFamily: 'monospace',
                fontSize: '1.2rem',
                fontWeight: 800,
                color: '#f8fafc',
                letterSpacing: '0.05em',
                background: 'rgba(0,0,0,0.3)',
                display: 'inline-block',
                padding: '6px 16px',
                borderRadius: '8px',
                margin: '10px 0',
              }}>
                {successReceipt.referenceNo}
              </div>
              <p style={{ color: '#94a3b8', fontSize: '0.9rem' }}>
                Scheduled for <strong>{formatSlotDateTime(successReceipt.startTime)}</strong> with {successReceipt.doctorName}.
              </p>
            </div>

            {/* Notification Dispatch Simulation Preview */}
            <div style={{
              background: 'rgba(15, 23, 42, 0.6)',
              border: '1px solid rgba(255, 255, 255, 0.08)',
              borderRadius: '12px',
              padding: '16px',
              fontSize: '0.85rem',
              marginBottom: '24px',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#38bdf8', fontWeight: 600, marginBottom: '6px' }}>
                <span>📱 Automated SMS & Email Dispatched</span>
              </div>
              <p style={{ color: '#cbd5e1', lineHeight: 1.5 }}>
                <em>"Amahoro Hospital: Confirmed appointment {successReceipt.referenceNo} with {successReceipt.doctorName} on {formatSlotDateTime(successReceipt.startTime)}."</em>
              </p>
            </div>

            <button
              onClick={onClose}
              className="btn-primary"
              style={{ width: '100%' }}
            >
              Done & View My Appointments
            </button>
          </div>
        ) : (
          <form onSubmit={handleBook}>
            {error && (
              <div style={{
                background: 'rgba(239, 68, 68, 0.15)',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                color: '#f87171',
                borderRadius: '8px',
                padding: '12px',
                fontSize: '0.85rem',
                marginBottom: '18px',
              }}>
                {error}
              </div>
            )}

            {/* Slot Picker */}
            <div className="form-group">
              <label className="form-label">
                Select Available Slot (14-Day Rolling Window):
              </label>
              {loadingSlots ? (
                <div style={{ color: '#94a3b8', fontSize: '0.85rem', padding: '10px 0' }}>
                  Loading available consultation slots...
                </div>
              ) : slots.length === 0 ? (
                <div style={{
                  background: 'rgba(245, 158, 11, 0.1)',
                  border: '1px solid rgba(245, 158, 11, 0.3)',
                  color: '#fbbf24',
                  padding: '12px',
                  borderRadius: '8px',
                  fontSize: '0.85rem',
                }}>
                  No open slots found for this doctor in the next 14 days.
                </div>
              ) : (
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))',
                  gap: '8px',
                  maxHeight: '180px',
                  overflowY: 'auto',
                  background: 'rgba(0,0,0,0.2)',
                  padding: '8px',
                  borderRadius: '10px',
                  border: '1px solid rgba(255,255,255,0.06)',
                }}>
                  {slots.map(s => {
                    const isSelected = s.id === selectedSlotId;
                    const urgent = isSlotUnderTwoHours(s.startTime);
                    return (
                      <button
                        type="button"
                        key={s.id}
                        onClick={() => setSelectedSlotId(s.id)}
                        style={{
                          background: isSelected ? 'var(--primary-teal)' : 'rgba(15, 23, 42, 0.8)',
                          color: isSelected ? '#ffffff' : '#f8fafc',
                          border: isSelected ? '1px solid var(--primary-teal-light)' : '1px solid rgba(255,255,255,0.08)',
                          borderRadius: '8px',
                          padding: '8px 10px',
                          fontSize: '0.8rem',
                          textAlign: 'left',
                        }}
                      >
                        <div style={{ fontWeight: 600 }}>{formatSlotDateTime(s.startTime)}</div>
                        {urgent && (
                          <span style={{ fontSize: '0.65rem', color: isSelected ? '#ffffff' : '#f59e0b', fontWeight: 700 }}>
                            &lt; 2H Urgent
                          </span>
                        )}
                      </button>
                    );
                  })}
                </div>
              )}
            </div>

            {/* 2-Hour Lead Time Warning */}
            {isUrgentSlot && (
              <div style={{
                background: 'rgba(245, 158, 11, 0.15)',
                border: '1px solid rgba(245, 158, 11, 0.3)',
                color: '#fbbf24',
                padding: '12px',
                borderRadius: '8px',
                fontSize: '0.85rem',
                marginBottom: '16px',
              }}>
                <strong>⚠️ 2-Hour Lead Time Rule (FR-BOOK-06):</strong>
                {isStaff ? (
                  <p style={{ marginTop: '4px' }}>
                    This slot starts in less than 2 hours. As authorized Reception/Admin staff, you may override the lead-time requirement with an operational reason below.
                  </p>
                ) : (
                  <p style={{ marginTop: '4px' }}>
                    Online patient booking is prohibited within 2 hours of slot start time. Please select a later slot or contact reception.
                  </p>
                )}
              </div>
            )}

            {/* Receptionist Override Reason (FR-RECEPT-01) */}
            {isUrgentSlot && isStaff && (
              <div className="form-group">
                <label className="form-label" style={{ color: '#fbbf24' }}>
                  * Receptionist Walk-in Override Reason:
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Walk-in patient triage approval by Dr. Gasana"
                  value={overrideReason}
                  onChange={(e) => setOverrideReason(e.target.value)}
                  className="form-input"
                  style={{ borderColor: 'rgba(245, 158, 11, 0.4)' }}
                />
              </div>
            )}

            {/* Reason for Visit */}
            <div className="form-group">
              <label className="form-label">* Chief Complaint / Reason for Visit:</label>
              <textarea
                required
                rows={3}
                placeholder="Describe your symptoms or reason for outpatient consultation (e.g. Chest tightness, hypertension follow-up)..."
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                className="form-textarea"
              />
            </div>

            {/* Payment Method / Gateway Stub */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '20px' }}>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">Payment Channel:</label>
                <select
                  value={paymentMethod}
                  onChange={(e) => setPaymentMethod(e.target.value)}
                  className="form-select"
                >
                  <option value="MTN_MOMO">MTN Mobile Money (*182#)</option>
                  <option value="AIRTEL_MONEY">Airtel Money (*500#)</option>
                  <option value="RECEPTION_CASH">Pay at Reception Desk</option>
                </select>
              </div>

              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">Gateway Stub Mode:</label>
                <select
                  value={paymentMode}
                  onChange={(e) => setPaymentMode(e.target.value)}
                  className="form-select"
                >
                  <option value="SUCCESS">SUCCESS (Instant Clearance)</option>
                  <option value="DECLINE">DECLINE (Simulate Insufficient Bal)</option>
                  <option value="TIMEOUT">TIMEOUT (Gateway Network Lag)</option>
                </select>
              </div>
            </div>

            {/* Submit Action */}
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end', marginTop: '24px' }}>
              <button
                type="button"
                onClick={onClose}
                className="btn-secondary"
                disabled={submitting}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn-primary"
                disabled={submitting || slots.length === 0 || (isUrgentSlot && !isStaff)}
              >
                {submitting ? 'Confirming with Hospital...' : `Confirm & Pay ${doctor.consultationFeeRwf.toLocaleString()} RWF`}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
