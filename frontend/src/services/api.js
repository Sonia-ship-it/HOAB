const BASE_URL = '/api/v1';

export const DEMO_PERSONAS = [
  {
    role: 'PATIENT',
    name: 'Patient Alpha (Jean Paul)',
    phone: '0788000001',
    password: 'Password123!',
    tag: 'Patient',
    color: '#0d9488'
  },
  {
    role: 'DOCTOR',
    name: 'Dr. Patrick Mugisha',
    phone: '0788000101',
    password: 'Password123!',
    tag: 'Cardiologist',
    color: '#3b82f6'
  },
  {
    role: 'RECEPTIONIST',
    name: 'Aline Uwase',
    phone: '0788000002',
    password: 'Password123!',
    tag: 'Receptionist',
    color: '#8b5cf6'
  },
  {
    role: 'ADMIN',
    name: 'Dr. Emmanuel Gasana',
    phone: '0788000000',
    password: 'Password123!',
    tag: 'Administrator',
    color: '#f59e0b'
  }
];

function getAuthHeaders() {
  const token = localStorage.getItem('hoabs_token');
  const headers = {
    'Content-Type': 'application/json',
    'X-Correlation-Id': crypto.randomUUID ? crypto.randomUUID() : 'fe-' + Date.now(),
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

async function handleResponse(res) {
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const errorMsg = data?.detail || data?.message || `HTTP ${res.status}: ${res.statusText}`;
    throw new Error(errorMsg);
  }
  return data;
}

export const api = {
  // Auth
  async login(phoneOrEmail, password) {
    const res = await fetch(`${BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phoneOrEmail, password }),
    });
    return handleResponse(res);
  },

  async register(data) {
    const res = await fetch(`${BASE_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse(res);
  },

  async getMe() {
    const res = await fetch(`${BASE_URL}/auth/me`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  // Departments & Doctors
  async getDepartments() {
    const res = await fetch(`${BASE_URL}/departments`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async searchDoctors(params = {}) {
    const query = new URLSearchParams();
    if (params.departmentId) query.set('departmentId', params.departmentId);
    if (params.query) query.set('query', params.query);
    const res = await fetch(`${BASE_URL}/doctors?${query.toString()}`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async getDoctor(id) {
    const res = await fetch(`${BASE_URL}/doctors/${id}`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async getDoctorSlots(doctorId, from, to) {
    const query = new URLSearchParams();
    if (from) query.set('from', from);
    if (to) query.set('to', to);
    const res = await fetch(`${BASE_URL}/doctors/${doctorId}/slots?${query.toString()}`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  // Appointments
  async bookAppointment(data) {
    const res = await fetch(`${BASE_URL}/appointments`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse(res);
  },

  async getMyAppointments() {
    const res = await fetch(`${BASE_URL}/appointments/my`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async cancelAppointment(id, reason, overrideReason) {
    const res = await fetch(`${BASE_URL}/appointments/${id}/cancel`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify({ reason, overrideReason }),
    });
    return handleResponse(res);
  },

  async rescheduleAppointment(id, newSlotId) {
    const res = await fetch(`${BASE_URL}/appointments/${id}/reschedule`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify({ newSlotId }),
    });
    return handleResponse(res);
  },

  // Doctor Console
  async getDoctorTodayAppointments() {
    const res = await fetch(`${BASE_URL}/doctor/today`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async updateAppointmentStatus(id, status, notes) {
    const res = await fetch(`${BASE_URL}/doctor/appointments/${id}/status`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify({ status, notes }),
    });
    return handleResponse(res);
  },

  async updateWorkingHours(hours) {
    const res = await fetch(`${BASE_URL}/doctor/my-schedule/hours`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(hours),
    });
    return handleResponse(res);
  },

  async addLeaveBlock(data) {
    const res = await fetch(`${BASE_URL}/doctor/my-schedule/leave`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse(res);
  },

  // Admin & Receptionist
  async getAdminReport(startDate, endDate) {
    const query = new URLSearchParams();
    if (startDate) query.set('startDate', startDate);
    if (endDate) query.set('endDate', endDate);
    const res = await fetch(`${BASE_URL}/admin/reports/summary?${query.toString()}`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async updateDepartmentHours(deptId, openTime, closeTime) {
    const res = await fetch(`${BASE_URL}/admin/departments/${deptId}/hours`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify({ openTime, closeTime }),
    });
    return handleResponse(res);
  },

  async createDoctor(data) {
    const res = await fetch(`${BASE_URL}/admin/doctors`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse(res);
  },
};
