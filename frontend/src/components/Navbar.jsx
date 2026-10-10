import React from 'react';
import { DEMO_PERSONAS } from '../services/api';

export default function Navbar({ currentUser, onSwitchPersona, onLogout, activeTab, setActiveTab }) {
  return (
    <header style={{
      background: 'rgba(10, 15, 29, 0.9)',
      backdropFilter: 'blur(20px)',
      borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
      position: 'sticky',
      top: 0,
      zIndex: 100,
    }}>
      {/* Emergency alert bar */}
      <div style={{
        background: 'linear-gradient(90deg, #0d9488, #0284c7)',
        color: '#ffffff',
        padding: '5px 24px',
        fontSize: '0.8rem',
        fontWeight: 600,
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{
            background: 'rgba(255,255,255,0.2)',
            padding: '1px 6px',
            borderRadius: '4px',
            fontSize: '0.7rem'
          }}>KIGALI EMERGENCY</span>
          <span>Ambulance & Trauma Hotline: <strong>912 / +250 788 123 456</strong></span>
        </div>
        <div>
          <span>Outpatient Hours: <strong>Mon-Fri 07:00 - 17:00 CAT</strong></span>
        </div>
      </div>

      <div className="app-container" style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '16px 24px',
        gap: '20px',
        flexWrap: 'wrap',
      }}>
        {/* Brand Logo */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px', cursor: 'pointer' }} onClick={() => setActiveTab('search')}>
          <div style={{
            width: '42px',
            height: '42px',
            borderRadius: '12px',
            background: 'linear-gradient(135deg, #14b8a6, #0d9488)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 4px 15px rgba(20, 184, 166, 0.35)',
          }}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#ffffff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M19 10.5h-5.5V5c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v5.5H5c-.83 0-1.5.67-1.5 1.5s.67 1.5 1.5 1.5h5.5V19c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5v-5.5H19c.83 0 1.5-.67 1.5-1.5s-.67-1.5-1.5-1.5z"/>
            </svg>
          </div>
          <div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, letterSpacing: '-0.02em', color: '#f8fafc' }}>
              Amahoro <span style={{ color: '#14b8a6' }}>HOABS</span>
            </div>
            <div style={{ fontSize: '0.75rem', color: '#94a3b8', letterSpacing: '0.04em' }}>
              AMAHORO GENERAL HOSPITAL · KIGALI
            </div>
          </div>
        </div>

        {/* Demo Persona Switcher */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          background: 'rgba(15, 23, 42, 0.6)',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          padding: '4px 8px',
          borderRadius: '9999px',
        }}>
          <span style={{ fontSize: '0.75rem', color: '#94a3b8', paddingLeft: '8px', fontWeight: 600 }}>Switch Persona:</span>
          {DEMO_PERSONAS.map(p => {
            const isSelected = currentUser?.phone === p.phone;
            return (
              <button
                key={p.phone}
                onClick={() => onSwitchPersona(p)}
                style={{
                  padding: '5px 12px',
                  borderRadius: '9999px',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  background: isSelected ? p.color : 'transparent',
                  color: isSelected ? '#ffffff' : '#94a3b8',
                  border: isSelected ? 'none' : '1px solid rgba(255,255,255,0.05)',
                  boxShadow: isSelected ? `0 2px 10px ${p.color}66` : 'none',
                }}
              >
                {p.tag}
              </button>
            );
          })}
        </div>

        {/* User state and actions */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {currentUser ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#f8fafc' }}>{currentUser.name}</div>
                <div style={{ fontSize: '0.7rem', color: '#14b8a6', fontWeight: 600 }}>{currentUser.role}</div>
              </div>
              <button
                onClick={onLogout}
                style={{
                  background: 'rgba(239, 68, 68, 0.15)',
                  border: '1px solid rgba(239, 68, 68, 0.3)',
                  color: '#f87171',
                  padding: '6px 12px',
                  borderRadius: '8px',
                  fontSize: '0.8rem',
                  fontWeight: 600,
                }}
              >
                Logout
              </button>
            </div>
          ) : (
            <button
              onClick={() => setActiveTab('login')}
              className="btn-primary"
              style={{ fontSize: '0.85rem', padding: '8px 16px' }}
            >
              Sign In
            </button>
          )}
        </div>
      </div>

      {/* Main Tab Bar */}
      <div className="app-container">
        <nav className="tab-nav">
          <button
            className={`tab-btn ${activeTab === 'search' ? 'active' : ''}`}
            onClick={() => setActiveTab('search')}
          >
            Doctor Directory & Booking
          </button>
          {currentUser?.role === 'PATIENT' && (
            <button
              className={`tab-btn ${activeTab === 'my-appointments' ? 'active' : ''}`}
              onClick={() => setActiveTab('my-appointments')}
            >
              My Appointments
            </button>
          )}
          {currentUser?.role === 'DOCTOR' && (
            <button
              className={`tab-btn ${activeTab === 'doctor-console' ? 'active' : ''}`}
              onClick={() => setActiveTab('doctor-console')}
            >
              Doctor Schedule & Queue
            </button>
          )}
          {(currentUser?.role === 'ADMIN' || currentUser?.role === 'RECEPTIONIST') && (
            <button
              className={`tab-btn ${activeTab === 'admin-portal' ? 'active' : ''}`}
              onClick={() => setActiveTab('admin-portal')}
            >
              Staff Console & Reports
            </button>
          )}
          <button
            className={`tab-btn ${activeTab === 'uat-pack' ? 'active' : ''}`}
            onClick={() => setActiveTab('uat-pack')}
            style={{ marginLeft: 'auto', color: activeTab === 'uat-pack' ? '#38bdf8' : '#94a3b8' }}
          >
            <span className="pulse-indicator" style={{ background: '#38bdf8' }}></span>
            Live UAT Verification Pack
          </button>
        </nav>
      </div>
    </header>
  );
}
