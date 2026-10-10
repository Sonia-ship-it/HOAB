import React, { useState, useEffect } from 'react';
import { api } from '../services/api';

export default function AdminConsole({ currentUser }) {
  const [report, setReport] = useState(null);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Department Hours Editor State
  const [selectedDeptId, setSelectedDeptId] = useState('');
  const [openTime, setOpenTime] = useState('07:00');
  const [closeTime, setCloseTime] = useState('17:00');
  const [deptSuccess, setDeptSuccess] = useState(null);
  const [deptError, setDeptError] = useState(null);

  // New Doctor State
  const [newDocStaffId, setNewDocStaffId] = useState('');
  const [newDocName, setNewDocName] = useState('');
  const [newDocPhone, setNewDocPhone] = useState('0788');
  const [newDocEmail, setNewDocEmail] = useState('');
  const [newDocDeptId, setNewDocDeptId] = useState('');
  const [newDocSpecialty, setNewDocSpecialty] = useState('');
  const [newDocFee, setNewDocFee] = useState(15000);
  const [newDocDuration, setNewDocDuration] = useState(20);
  const [docSuccess, setDocSuccess] = useState(null);
  const [docError, setDocError] = useState(null);

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    try {
      setLoading(true);
      setError(null);
      const [rep, depts] = await Promise.all([
        api.getAdminReport(),
        api.getDepartments(),
      ]);
      setReport(rep);
      setDepartments(depts || []);
      if (depts && depts.length > 0) {
        setSelectedDeptId(depts[0].id);
        setNewDocDeptId(depts[0].id);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleUpdateDeptHours(e) {
    e.preventDefault();
    try {
      setDeptError(null);
      setDeptSuccess(null);
      await api.updateDepartmentHours(
        selectedDeptId,
        openTime.length === 5 ? openTime + ':00' : openTime,
        closeTime.length === 5 ? closeTime + ':00' : closeTime
      );
      setDeptSuccess('Department operating hours updated successfully!');
      await loadData();
    } catch (err) {
      setDeptError(err.message);
    }
  }

  async function handleCreateDoctor(e) {
    e.preventDefault();
    try {
      setDocError(null);
      setDocSuccess(null);
      await api.createDoctor({
        staffId: newDocStaffId,
        fullName: newDocName,
        phone: newDocPhone,
        email: newDocEmail,
        departmentId: newDocDeptId,
        specialty: newDocSpecialty,
        consultationFeeRwf: Number(newDocFee),
        slotDurationMinutes: Number(newDocDuration),
      });
      setDocSuccess(`Dr. ${newDocName} (${newDocStaffId}) onboarded and slots generated!`);
      setNewDocStaffId('');
      setNewDocName('');
      setNewDocPhone('0788');
      setNewDocEmail('');
      setNewDocSpecialty('');
    } catch (err) {
      setDocError(err.message);
    }
  }

  function handleExportCsv() {
    if (!report) return;
    const csvContent = "data:text/csv;charset=utf-8,"
      + "Metric,Value\n"
      + `Total Appointments,${report.totalAppointments}\n`
      + `Confirmed Count,${report.confirmedCount}\n`
      + `Completed Count,${report.completedCount}\n`
      + `No-Show Count,${report.noShowCount}\n`
      + `Cancelled Count,${report.cancelledCount}\n`
      + `Completion Rate,${report.completionRatePercent}%\n`
      + `No-Show Rate,${report.noShowRatePercent}%\n`
      + `Total Revenue RWF,${report.totalRevenueRwf}\n`;

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `hoabs_operational_report_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  return (
    <div>
      <div style={{ marginBottom: '28px' }}>
        <h2 style={{ fontSize: '1.6rem', color: '#f8fafc', marginBottom: '4px' }}>
          Hospital Administration & Reception Console
        </h2>
        <p style={{ color: '#94a3b8', fontSize: '0.9rem' }}>
          Real-time outpatient performance metrics, departmental schedule bounds, and clinical staff onboarding.
        </p>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: '#94a3b8' }}>
          <div className="pulse-indicator" style={{ width: '12px', height: '12px', marginBottom: '10px' }}></div>
          <div>Loading administrative analytics...</div>
        </div>
      ) : error ? (
        <div className="glass-panel" style={{ padding: '24px', color: '#f87171' }}>
          Failed to load admin data: {error}
        </div>
      ) : (
        <>
          {/* Executive KPI Metric Cards */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: '16px',
            marginBottom: '28px',
          }}>
            <div className="glass-panel" style={{ padding: '20px' }}>
              <div style={{ fontSize: '0.8rem', color: '#94a3b8', marginBottom: '6px' }}>TOTAL BOOKINGS</div>
              <div style={{ fontSize: '2rem', fontWeight: 800, color: '#f8fafc' }}>
                {report?.totalAppointments || 0}
              </div>
              <div style={{ fontSize: '0.75rem', color: '#38bdf8' }}>30-day outpatient total</div>
            </div>

            <div className="glass-panel" style={{ padding: '20px' }}>
              <div style={{ fontSize: '0.8rem', color: '#94a3b8', marginBottom: '6px' }}>COMPLETION RATE</div>
              <div style={{ fontSize: '2rem', fontWeight: 800, color: '#10b981' }}>
                {report?.completionRatePercent || 0}%
              </div>
              <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                {report?.completedCount || 0} consultations delivered
              </div>
            </div>

            <div className="glass-panel" style={{ padding: '20px' }}>
              <div style={{ fontSize: '0.8rem', color: '#94a3b8', marginBottom: '6px' }}>NO-SHOW RATE</div>
              <div style={{ fontSize: '2rem', fontWeight: 800, color: '#f59e0b' }}>
                {report?.noShowRatePercent || 0}%
              </div>
              <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                {report?.noShowCount || 0} missed visits
              </div>
            </div>

            <div className="glass-panel" style={{ padding: '20px' }}>
              <div style={{ fontSize: '0.8rem', color: '#94a3b8', marginBottom: '6px' }}>ESTIMATED REVENUE</div>
              <div style={{ fontSize: '2rem', fontWeight: 800, color: '#2dd4bf' }}>
                {(report?.totalRevenueRwf || 0).toLocaleString()}
              </div>
              <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>RWF Consultation Fees</div>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '28px' }}>
            <button onClick={handleExportCsv} className="btn-secondary" style={{ gap: '8px' }}>
              📥 Export Operational Report (CSV)
            </button>
          </div>

          {/* Department Operating Hours Editor & New Doctor Forms */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
            gap: '24px',
          }}>
            {/* Department Hours */}
            <div className="glass-panel" style={{ padding: '28px' }}>
              <h3 style={{ color: '#f8fafc', marginBottom: '6px' }}>Department Operating Hours</h3>
              <p style={{ color: '#94a3b8', fontSize: '0.85rem', marginBottom: '20px' }}>
                Enforces boundaries for doctor recurring schedule definitions (FR-ADMIN-01).
              </p>

              {deptSuccess && (
                <div style={{ color: '#34d399', background: 'rgba(16,185,129,0.1)', padding: '10px', borderRadius: '8px', marginBottom: '14px', fontSize: '0.85rem' }}>
                  {deptSuccess}
                </div>
              )}
              {deptError && (
                <div style={{ color: '#f87171', background: 'rgba(239,68,68,0.1)', padding: '10px', borderRadius: '8px', marginBottom: '14px', fontSize: '0.85rem' }}>
                  {deptError}
                </div>
              )}

              <form onSubmit={handleUpdateDeptHours}>
                <div className="form-group">
                  <label className="form-label">Select Department:</label>
                  <select
                    value={selectedDeptId}
                    onChange={(e) => setSelectedDeptId(e.target.value)}
                    className="form-select"
                  >
                    {departments.map(d => (
                      <option key={d.id} value={d.id}>
                        {d.name} ({d.openingTime?.substring(0, 5)} - {d.closingTime?.substring(0, 5)})
                      </option>
                    ))}
                  </select>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '16px' }}>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label">Opening Time:</label>
                    <input
                      type="time"
                      value={openTime}
                      onChange={(e) => setOpenTime(e.target.value)}
                      className="form-input"
                    />
                  </div>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label">Closing Time:</label>
                    <input
                      type="time"
                      value={closeTime}
                      onChange={(e) => setCloseTime(e.target.value)}
                      className="form-input"
                    />
                  </div>
                </div>

                <button type="submit" className="btn-primary" style={{ width: '100%' }}>
                  Save Department Hours
                </button>
              </form>
            </div>

            {/* Doctor Onboarding */}
            <div className="glass-panel" style={{ padding: '28px' }}>
              <h3 style={{ color: '#f8fafc', marginBottom: '6px' }}>Onboard Specialist Doctor</h3>
              <p style={{ color: '#94a3b8', fontSize: '0.85rem', marginBottom: '20px' }}>
                Creates doctor profile, user account, and initializes rolling 14-day schedule.
              </p>

              {docSuccess && (
                <div style={{ color: '#34d399', background: 'rgba(16,185,129,0.1)', padding: '10px', borderRadius: '8px', marginBottom: '14px', fontSize: '0.85rem' }}>
                  {docSuccess}
                </div>
              )}
              {docError && (
                <div style={{ color: '#f87171', background: 'rgba(239,68,68,0.1)', padding: '10px', borderRadius: '8px', marginBottom: '14px', fontSize: '0.85rem' }}>
                  {docError}
                </div>
              )}

              <form onSubmit={handleCreateDoctor}>
                <div style={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: '10px' }}>
                  <div className="form-group">
                    <label className="form-label">Staff ID:</label>
                    <input
                      type="text"
                      required
                      placeholder="DOC-005"
                      value={newDocStaffId}
                      onChange={(e) => setNewDocStaffId(e.target.value)}
                      className="form-input"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Full Name:</label>
                    <input
                      type="text"
                      required
                      placeholder="Dr. Claudine Mukamana"
                      value={newDocName}
                      onChange={(e) => setNewDocName(e.target.value)}
                      className="form-input"
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                  <div className="form-group">
                    <label className="form-label">Rwandan Phone:</label>
                    <input
                      type="text"
                      required
                      placeholder="0788123456"
                      value={newDocPhone}
                      onChange={(e) => setNewDocPhone(e.target.value)}
                      className="form-input"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Specialty:</label>
                    <input
                      type="text"
                      required
                      placeholder="Pediatric Cardiology"
                      value={newDocSpecialty}
                      onChange={(e) => setNewDocSpecialty(e.target.value)}
                      className="form-input"
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Department:</label>
                  <select
                    value={newDocDeptId}
                    onChange={(e) => setNewDocDeptId(e.target.value)}
                    className="form-select"
                  >
                    {departments.map(d => (
                      <option key={d.id} value={d.id}>{d.name}</option>
                    ))}
                  </select>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '16px' }}>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label">Fee (RWF):</label>
                    <input
                      type="number"
                      value={newDocFee}
                      onChange={(e) => setNewDocFee(e.target.value)}
                      className="form-input"
                    />
                  </div>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label">Slot Duration (Min):</label>
                    <input
                      type="number"
                      value={newDocDuration}
                      onChange={(e) => setNewDocDuration(e.target.value)}
                      className="form-input"
                    />
                  </div>
                </div>

                <button type="submit" className="btn-primary" style={{ width: '100%' }}>
                  Onboard Doctor
                </button>
              </form>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
