import React from 'react';

export default function HeroBanner() {
  return (
    <div style={{
      background: 'linear-gradient(135deg, rgba(13, 148, 136, 0.12) 0%, rgba(15, 23, 42, 0.7) 100%)',
      border: '1px solid rgba(20, 184, 166, 0.25)',
      borderRadius: '24px',
      padding: '36px',
      margin: '28px 0',
      position: 'relative',
      overflow: 'hidden',
    }}>
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
        gap: '30px',
        alignItems: 'center',
      }}>
        <div>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            background: 'rgba(20, 184, 166, 0.15)',
            border: '1px solid rgba(20, 184, 166, 0.3)',
            borderRadius: '9999px',
            padding: '4px 14px',
            marginBottom: '16px',
            fontSize: '0.8rem',
            color: '#2dd4bf',
            fontWeight: 600,
          }}>
            <span className="pulse-indicator"></span>
            Outpatient Booking Live · Kigali, Rwanda
          </div>
          <h1 style={{
            fontSize: '2.5rem',
            lineHeight: 1.15,
            marginBottom: '14px',
            color: '#f8fafc',
            fontWeight: 800,
          }}>
            Quality Outpatient Care, <br />
            <span style={{
              background: 'linear-gradient(90deg, #14b8a6, #38bdf8)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
            }}>
              Zero Waiting Lines.
            </span>
          </h1>
          <p style={{
            color: '#94a3b8',
            fontSize: '1rem',
            lineHeight: 1.6,
            marginBottom: '24px',
            maxWidth: '540px',
          }}>
            Book confirmed consultations with specialist physicians at Amahoro General Hospital.
            Real-time slot availability, instant SMS reminders, and seamless MoMo payment stub integration.
          </p>

          <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap' }}>
            <div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#f8fafc' }}>14 Days</div>
              <div style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Rolling Slot Horizon</div>
            </div>
            <div style={{ width: '1px', background: 'rgba(255,255,255,0.1)' }}></div>
            <div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#2dd4bf' }}>&lt; 200 ms</div>
              <div style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Atomic Concurrency</div>
            </div>
            <div style={{ width: '1px', background: 'rgba(255,255,255,0.1)' }}></div>
            <div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#38bdf8' }}>100% Pass</div>
              <div style={{ fontSize: '0.8rem', color: '#94a3b8' }}>UAT Test Compliance</div>
            </div>
          </div>
        </div>

        {/* Feature Highlights Grid */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '14px',
        }}>
          <div className="glass-panel" style={{ padding: '18px' }}>
            <div style={{ fontSize: '1.5rem', marginBottom: '8px' }}>🏥</div>
            <h3 style={{ fontSize: '1rem', color: '#f8fafc', marginBottom: '4px' }}>4 Outpatient Clinics</h3>
            <p style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Cardiology, Pediatrics, Orthopedics & General Medicine</p>
          </div>
          <div className="glass-panel" style={{ padding: '18px' }}>
            <div style={{ fontSize: '1.5rem', marginBottom: '8px' }}>⚡</div>
            <h3 style={{ fontSize: '1rem', color: '#f8fafc', marginBottom: '4px' }}>2-Hour Policy</h3>
            <p style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Strict lead time guardrails with audited receptionist override</p>
          </div>
          <div className="glass-panel" style={{ padding: '18px' }}>
            <div style={{ fontSize: '1.5rem', marginBottom: '8px' }}>📱</div>
            <h3 style={{ fontSize: '1rem', color: '#f8fafc', marginBottom: '4px' }}>MTN / Airtel MoMo</h3>
            <p style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Standard consultation fee collection with gateway stub modes</p>
          </div>
          <div className="glass-panel" style={{ padding: '18px' }}>
            <div style={{ fontSize: '1.5rem', marginBottom: '8px' }}>🔔</div>
            <h3 style={{ fontSize: '1rem', color: '#f8fafc', marginBottom: '4px' }}>SMS & Email Alerts</h3>
            <p style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Instant booking confirmation, cancellation & reminder notices</p>
          </div>
        </div>
      </div>
    </div>
  );
}
