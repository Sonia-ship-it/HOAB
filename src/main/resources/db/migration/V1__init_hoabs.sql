-- V1__init_hoabs.sql: Hospital Online Appointment Booking System (HOABS) Schema

CREATE TABLE users (
    id UUID PRIMARY KEY,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(255) UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    date_of_birth DATE,
    insurance_note TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT users_role_chk CHECK (role IN ('PATIENT', 'DOCTOR', 'RECEPTIONIST', 'ADMIN')),
    CONSTRAINT users_status_chk CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED'))
);

CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_email ON users(email);

CREATE TABLE departments (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    opening_time TIME NOT NULL,
    closing_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE doctors (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    staff_id VARCHAR(64) NOT NULL UNIQUE,
    department_id UUID NOT NULL REFERENCES departments(id),
    specialty VARCHAR(120) NOT NULL,
    slot_duration_minutes INTEGER NOT NULL DEFAULT 20,
    consultation_fee_rwf BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_doctors_dept ON doctors(department_id);
CREATE INDEX idx_doctors_active ON doctors(active);

CREATE TABLE working_hours (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CONSTRAINT working_hours_day_chk CHECK (day_of_week BETWEEN 1 AND 7)
);

CREATE TABLE leave_blocks (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE slots (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    is_open BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT slots_doc_start_uniq UNIQUE (doctor_id, start_time)
);

CREATE INDEX idx_slots_doc_open ON slots(doctor_id, is_open, start_time);

CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    reference_no VARCHAR(32) NOT NULL UNIQUE,
    patient_id UUID NOT NULL REFERENCES users(id),
    doctor_id UUID NOT NULL REFERENCES doctors(id),
    slot_id UUID NOT NULL REFERENCES slots(id),
    reason VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    cancellation_reason TEXT,
    override_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT appointments_status_chk CHECK (status IN (
        'REQUESTED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'RESCHEDULED', 'NO_SHOW'
    ))
);

CREATE INDEX idx_appointments_patient ON appointments(patient_id, status);
CREATE INDEX idx_appointments_doctor ON appointments(doctor_id, status);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    appointment_id UUID REFERENCES appointments(id) ON DELETE SET NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel VARCHAR(16) NOT NULL,
    type VARCHAR(32) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT notifications_channel_chk CHECK (channel IN ('SMS', 'EMAIL')),
    CONSTRAINT notifications_type_chk CHECK (type IN (
        'CONFIRMATION', 'REMINDER', 'CANCELLATION', 'RESCHEDULE', 'SECURITY_ALERT'
    )),
    CONSTRAINT notifications_status_chk CHECK (status IN ('SENT', 'FAILED', 'RETRIED'))
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    appointment_id UUID REFERENCES appointments(id) ON DELETE SET NULL,
    amount_rwf BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    gateway_ref VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT payments_provider_chk CHECK (provider IN ('MTN_MOMO', 'CARD')),
    CONSTRAINT payments_status_chk CHECK (status IN ('PENDING', 'SUCCEEDED', 'DECLINED', 'TIMED_OUT'))
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(64) NOT NULL,
    details TEXT,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
