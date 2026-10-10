import React, { useState, useEffect } from 'react';
import { api } from '../services/api';

export default function DoctorSearch({ onSelectDoctorToBook }) {
  const [departments, setDepartments] = useState([]);
  const [selectedDeptId, setSelectedDeptId] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadDepartments();
  }, []);

  useEffect(() => {
    loadDoctors();
  }, [selectedDeptId, searchQuery]);

  async function loadDepartments() {
    try {
      const data = await api.getDepartments();
      setDepartments(data || []);
    } catch (err) {
      console.error('Failed to load departments', err);
    }
  }

  async function loadDoctors() {
    try {
      setLoading(true);
      setError(null);
      const data = await api.searchDoctors({
        departmentId: selectedDeptId || undefined,
        query: searchQuery || undefined,
      });
      setDoctors(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function formatSlotTime(instantStr) {
    if (!instantStr) return 'No slots currently open';
    const date = new Date(instantStr);
    return date.toLocaleString('en-GB', {
      timeZone: 'Africa/Kigali',
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    }) + ' CAT';
  }

  return (
    <div>
      {/* Search Header and Department Filters */}
      <div className="glass-panel" style={{ padding: '24px', marginBottom: '28px' }}>
        <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', marginBottom: '20px' }}>
          <div style={{ flex: '1 1 300px', position: 'relative' }}>
            <input
              type="text"
              placeholder="Search by doctor name (e.g. Mugisha), staff ID or specialty..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="form-input"
              style={{ width: '100%', paddingLeft: '40px' }}
            />
            <svg
              style={{ position: 'absolute', left: '14px', top: '12px', color: '#94a3b8' }}
              width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
            >
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </div>
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="btn-secondary"
              style={{ padding: '8px 14px' }}
            >
              Clear
            </button>
          )}
        </div>

        {/* Department Filters */}
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'center' }}>
          <span style={{ fontSize: '0.8rem', color: '#94a3b8', fontWeight: 600 }}>Filter Clinic:</span>
          <button
            onClick={() => setSelectedDeptId('')}
            style={{
              padding: '6px 14px',
              borderRadius: '9999px',
              fontSize: '0.85rem',
              fontWeight: 600,
              background: selectedDeptId === '' ? 'var(--primary-teal)' : 'rgba(255,255,255,0.05)',
              color: selectedDeptId === '' ? '#ffffff' : '#94a3b8',
              border: selectedDeptId === '' ? 'none' : '1px solid rgba(255,255,255,0.08)',
            }}
          >
            All Departments ({doctors.length})
          </button>
          {departments.map(dept => (
            <button
              key={dept.id}
              onClick={() => setSelectedDeptId(dept.id)}
              style={{
                padding: '6px 14px',
                borderRadius: '9999px',
                fontSize: '0.85rem',
                fontWeight: 600,
                background: selectedDeptId === dept.id ? 'var(--primary-teal)' : 'rgba(255,255,255,0.05)',
                color: selectedDeptId === dept.id ? '#ffffff' : '#94a3b8',
                border: selectedDeptId === dept.id ? 'none' : '1px solid rgba(255,255,255,0.08)',
              }}
            >
              {dept.name}
            </button>
          ))}
        </div>
      </div>

      {/* Doctor List Results */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: '#94a3b8' }}>
          <div className="pulse-indicator" style={{ width: '14px', height: '14px', marginBottom: '12px' }}></div>
          <div>Loading Outpatient Specialists...</div>
        </div>
      ) : error ? (
        <div className="glass-panel" style={{ padding: '24px', borderColor: 'rgba(239, 68, 68, 0.4)', color: '#f87171' }}>
          Failed to load doctors: {error}
        </div>
      ) : doctors.length === 0 ? (
        <div className="glass-panel" style={{ textAlign: 'center', padding: '48px 24px' }}>
          <div style={{ fontSize: '2rem', marginBottom: '10px' }}>🔍</div>
          <h3 style={{ color: '#f8fafc', marginBottom: '6px' }}>No Doctors Found</h3>
          <p style={{ color: '#94a3b8', fontSize: '0.9rem' }}>Try clearing your search term or selecting another department.</p>
        </div>
      ) : (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))',
          gap: '24px',
        }}>
          {doctors.map(doc => (
            <div key={doc.id} className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '14px' }}>
                  <div style={{ display: 'flex', gap: '14px', alignItems: 'center' }}>
                    <div style={{
                      width: '48px',
                      height: '48px',
                      borderRadius: '12px',
                      background: 'linear-gradient(135deg, rgba(20,184,166,0.2), rgba(59,130,246,0.2))',
                      border: '1px solid rgba(20,184,166,0.3)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '1.2rem',
                    }}>
                      👨‍⚕️
                    </div>
                    <div>
                      <h3 style={{ fontSize: '1.15rem', color: '#f8fafc', marginBottom: '2px' }}>
                        {doc.fullName}
                      </h3>
                      <div style={{ fontSize: '0.8rem', color: '#14b8a6', fontWeight: 600 }}>
                        {doc.staffId} · {doc.specialty}
                      </div>
                    </div>
                  </div>
                  <span className="badge badge-confirmed" style={{ fontSize: '0.7rem' }}>
                    {doc.departmentName}
                  </span>
                </div>

                <div style={{
                  background: 'rgba(0, 0, 0, 0.25)',
                  borderRadius: '10px',
                  padding: '12px',
                  marginBottom: '18px',
                  fontSize: '0.85rem',
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                    <span style={{ color: '#94a3b8' }}>Consultation Fee:</span>
                    <strong style={{ color: '#f8fafc' }}>{doc.consultationFeeRwf.toLocaleString()} RWF</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                    <span style={{ color: '#94a3b8' }}>Session Length:</span>
                    <span style={{ color: '#f8fafc' }}>{doc.slotDurationMinutes} Minutes</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: '#94a3b8' }}>Next Open Slot:</span>
                    <span style={{ color: doc.nextAvailableSlot ? '#2dd4bf' : '#f59e0b', fontWeight: 600 }}>
                      {formatSlotTime(doc.nextAvailableSlot)}
                    </span>
                  </div>
                </div>
              </div>

              <button
                onClick={() => onSelectDoctorToBook(doc)}
                className="btn-primary"
                style={{ width: '100%' }}
                disabled={!doc.active}
              >
                <span>📅 Select Time Slot & Book</span>
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
