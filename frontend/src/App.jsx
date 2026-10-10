import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import HeroBanner from './components/HeroBanner';
import DoctorSearch from './components/DoctorSearch';
import BookingModal from './components/BookingModal';
import MyAppointments from './components/MyAppointments';
import DoctorConsole from './components/DoctorConsole';
import AdminConsole from './components/AdminConsole';
import UatDashboard from './components/UatDashboard';
import { api, DEMO_PERSONAS } from './services/api';

export default function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const [activeTab, setActiveTab] = useState('search');
  const [selectedDoctorToBook, setSelectedDoctorToBook] = useState(null);
  const [toastMessage, setToastMessage] = useState(null);

  // Initialize with Patient Alpha for instant out-of-the-box demo
  useEffect(() => {
    switchPersona(DEMO_PERSONAS[0]);
  }, []);

  async function switchPersona(persona) {
    try {
      showToast(`Signing in as ${persona.name} (${persona.tag})...`);
      const auth = await api.login(persona.phone, persona.password);
      localStorage.setItem('hoabs_token', auth.token);
      setCurrentUser(auth);

      // Route tab appropriately based on role
      if (persona.role === 'DOCTOR') {
        setActiveTab('doctor-console');
      } else if (persona.role === 'ADMIN' || persona.role === 'RECEPTIONIST') {
        setActiveTab('admin-portal');
      } else {
        setActiveTab('search');
      }
      showToast(`Active Persona: ${auth.name} [${auth.role}]`);
    } catch (err) {
      console.error('Failed to switch persona', err);
      showToast('Authentication failed: ' + err.message, true);
    }
  }

  function handleLogout() {
    localStorage.removeItem('hoabs_token');
    setCurrentUser(null);
    setActiveTab('search');
    showToast('Signed out successfully');
  }

  function showToast(msg, isError = false) {
    setToastMessage({ text: msg, isError });
    setTimeout(() => {
      setToastMessage(null);
    }, 4000);
  }

  function handleBookingSuccess(receipt) {
    showToast(`Appointment ${receipt.referenceNo} confirmed with Dr. ${receipt.doctorName}!`);
    setActiveTab('my-appointments');
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Toast Alert */}
      {toastMessage && (
        <div style={{
          position: 'fixed',
          bottom: '24px',
          right: '24px',
          zIndex: 2000,
          background: toastMessage.isError ? 'rgba(239, 68, 68, 0.95)' : 'rgba(15, 23, 42, 0.95)',
          border: toastMessage.isError ? '1px solid #ef4444' : '1px solid #14b8a6',
          color: '#ffffff',
          padding: '12px 20px',
          borderRadius: '10px',
          boxShadow: '0 10px 25px rgba(0,0,0,0.5)',
          fontSize: '0.9rem',
          fontWeight: 600,
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          backdropFilter: 'blur(8px)',
        }}>
          <span>{toastMessage.isError ? '⚠️' : '🔔'}</span>
          <span>{toastMessage.text}</span>
        </div>
      )}

      {/* Navigation */}
      <Navbar
        currentUser={currentUser}
        onSwitchPersona={switchPersona}
        onLogout={handleLogout}
        activeTab={activeTab}
        setActiveTab={setActiveTab}
      />

      {/* Main Content Area */}
      <main className="app-container" style={{ flex: 1, paddingBottom: '60px' }}>
        {activeTab === 'search' && (
          <>
            <HeroBanner />
            <DoctorSearch
              onSelectDoctorToBook={(doc) => setSelectedDoctorToBook(doc)}
            />
          </>
        )}

        {activeTab === 'my-appointments' && (
          <div style={{ marginTop: '28px' }}>
            <MyAppointments
              onOpenBookingForDoctor={(doc) => setSelectedDoctorToBook(doc)}
            />
          </div>
        )}

        {activeTab === 'doctor-console' && (
          <div style={{ marginTop: '28px' }}>
            <DoctorConsole currentUser={currentUser} />
          </div>
        )}

        {activeTab === 'admin-portal' && (
          <div style={{ marginTop: '28px' }}>
            <AdminConsole currentUser={currentUser} />
          </div>
        )}

        {activeTab === 'uat-pack' && (
          <div style={{ marginTop: '28px' }}>
            <UatDashboard />
          </div>
        )}
      </main>

      {/* Booking Modal */}
      {selectedDoctorToBook && (
        <BookingModal
          doctor={selectedDoctorToBook}
          currentUser={currentUser}
          onClose={() => setSelectedDoctorToBook(null)}
          onBookingSuccess={handleBookingSuccess}
        />
      )}

      {/* Footer */}
      <footer style={{
        background: 'rgba(10, 15, 29, 0.95)',
        borderTop: '1px solid rgba(255, 255, 255, 0.08)',
        padding: '36px 0',
        marginTop: 'auto',
      }}>
        <div className="app-container" style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '20px',
        }}>
          <div>
            <div style={{ fontWeight: 800, fontSize: '1.1rem', color: '#f8fafc', marginBottom: '4px' }}>
              Amahoro General Hospital (HOABS)
            </div>
            <div style={{ color: '#94a3b8', fontSize: '0.85rem' }}>
              Outpatient Department · Kigali, Rwanda · Academic Year 2026–2027
            </div>
            <div style={{ color: '#64748b', fontSize: '0.75rem', marginTop: '4px' }}>
              Compliant with Document HOAB-SRS-2026-001 & HOAB-UAT-2026-001 v1.0
            </div>
          </div>

          <div style={{ display: 'flex', gap: '20px', fontSize: '0.85rem', color: '#94a3b8' }}>
            <span>🚑 Hotline: 912</span>
            <span>📍 KG 543 St, Kigali</span>
            <span>✉️ contact@amahoro.gov.rw</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
