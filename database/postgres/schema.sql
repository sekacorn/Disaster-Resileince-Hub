-- DisasterResilienceHub PostgreSQL Database Schema
-- Includes user management with roles (User, Moderator, Admin)
-- SSO integration support with MFA

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- USER MANAGEMENT & AUTHENTICATION
-- ============================================

-- User roles enum
CREATE TYPE user_role AS ENUM ('USER', 'MODERATOR', 'ADMIN');

-- Account types
CREATE TYPE account_type AS ENUM ('STANDARD', 'ENTERPRISE', 'NONPROFIT');

-- MFA types
CREATE TYPE mfa_type AS ENUM ('TOTP', 'SMS', 'EMAIL', 'AUTHENTICATOR_APP');

-- Users table with SSO and MFA support
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255), -- NULL for SSO-only users
    role user_role DEFAULT 'USER' NOT NULL,
    account_type account_type DEFAULT 'STANDARD' NOT NULL,

    -- Profile information
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    organization VARCHAR(255),
    mbti_type VARCHAR(4), -- For MBTI-tailored UX

    -- SSO fields
    sso_provider VARCHAR(50), -- 'SAML', 'OAUTH2', 'OIDC', NULL for standard auth
    sso_subject_id VARCHAR(255), -- Unique ID from SSO provider
    sso_metadata JSONB, -- Additional SSO attributes

    -- MFA fields
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_type mfa_type,
    mfa_secret VARCHAR(255), -- Encrypted TOTP secret or other MFA data
    mfa_backup_codes TEXT[], -- Encrypted backup codes

    -- Account status
    is_active BOOLEAN DEFAULT TRUE,
    is_email_verified BOOLEAN DEFAULT FALSE,
    is_locked BOOLEAN DEFAULT FALSE,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    password_changed_at TIMESTAMP,

    -- Audit
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

-- Index for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_sso_subject ON users(sso_subject_id);
CREATE INDEX idx_users_role ON users(role);

-- SSO providers configuration
CREATE TABLE sso_providers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider_name VARCHAR(100) UNIQUE NOT NULL,
    provider_type VARCHAR(50) NOT NULL, -- 'SAML', 'OAUTH2', 'OIDC'

    -- Configuration
    metadata_url VARCHAR(500),
    entity_id VARCHAR(255),
    sso_url VARCHAR(500),
    certificate TEXT,
    client_id VARCHAR(255),
    client_secret VARCHAR(255),
    authorization_endpoint VARCHAR(500),
    token_endpoint VARCHAR(500),
    user_info_endpoint VARCHAR(500),

    -- Settings
    is_active BOOLEAN DEFAULT TRUE,
    auto_provision_users BOOLEAN DEFAULT FALSE,
    default_role user_role DEFAULT 'USER',

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User sessions for JWT and session management
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    session_token VARCHAR(500) UNIQUE NOT NULL,
    refresh_token VARCHAR(500) UNIQUE,
    ip_address VARCHAR(45),
    user_agent TEXT,

    -- Session details
    is_active BOOLEAN DEFAULT TRUE,
    expires_at TIMESTAMP NOT NULL,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_sessions_token ON user_sessions(session_token);

-- MFA verification attempts
CREATE TABLE mfa_verifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    verification_code VARCHAR(10),
    mfa_type mfa_type NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE,
    attempts INTEGER DEFAULT 0,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mfa_user ON mfa_verifications(user_id);

-- Audit log for security events
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    resource_id UUID,
    ip_address VARCHAR(45),
    user_agent TEXT,
    details JSONB,
    success BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

-- ============================================
-- DISASTER DATA MODELS
-- ============================================

-- Environmental data (from NOAA, USGS, etc.)
CREATE TABLE environmental_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source VARCHAR(100) NOT NULL, -- 'NOAA', 'USGS', etc.
    data_type VARCHAR(50) NOT NULL, -- 'weather', 'seismic', 'tsunami', etc.

    -- Geographic data
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    location_name VARCHAR(255),

    -- Data payload
    raw_data JSONB NOT NULL,
    severity VARCHAR(20), -- 'low', 'medium', 'high', 'critical'

    -- Timestamps
    data_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_env_data_type ON environmental_data(data_type);
CREATE INDEX idx_env_location ON environmental_data(latitude, longitude);
CREATE INDEX idx_env_timestamp ON environmental_data(data_timestamp);

-- Community data (from OpenStreetMap, etc.)
CREATE TABLE community_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source VARCHAR(100) NOT NULL,

    -- Geographic boundaries (GeoJSON)
    geojson JSONB NOT NULL,

    -- Community details
    name VARCHAR(255),
    population INTEGER,
    infrastructure_type VARCHAR(100), -- 'hospital', 'school', 'shelter', etc.

    -- Metadata
    metadata JSONB,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_community_type ON community_data(infrastructure_type);

-- Individual health data (FHIR-compliant)
CREATE TABLE individual_health_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    -- FHIR resource data
    fhir_resource_type VARCHAR(50) NOT NULL,
    fhir_resource JSONB NOT NULL,

    -- Privacy
    is_encrypted BOOLEAN DEFAULT TRUE,
    consent_given BOOLEAN DEFAULT FALSE,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_health_user ON individual_health_data(user_id);
CREATE INDEX idx_health_type ON individual_health_data(fhir_resource_type);

-- User locations for evacuation planning
CREATE TABLE user_locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    address TEXT,
    is_current BOOLEAN DEFAULT TRUE,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_location_user ON user_locations(user_id);
CREATE INDEX idx_location_coords ON user_locations(latitude, longitude);

-- ============================================
-- DISASTER VISUALIZATIONS & PREDICTIONS
-- ============================================

-- Disaster maps (3D visualizations)
CREATE TABLE disaster_maps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,

    -- Map details
    name VARCHAR(255) NOT NULL,
    description TEXT,
    disaster_type VARCHAR(50) NOT NULL,

    -- Geographic area
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    radius_km DECIMAL(10, 2),

    -- Visualization data
    visualization_data JSONB NOT NULL,
    map_metadata JSONB,

    -- Sharing
    is_public BOOLEAN DEFAULT FALSE,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_maps_user ON disaster_maps(user_id);
CREATE INDEX idx_maps_type ON disaster_maps(disaster_type);

-- Evacuation plans
CREATE TABLE evacuation_plans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    -- Plan details
    plan_name VARCHAR(255) NOT NULL,
    disaster_type VARCHAR(50) NOT NULL,

    -- Route information
    start_latitude DECIMAL(10, 8) NOT NULL,
    start_longitude DECIMAL(11, 8) NOT NULL,
    end_latitude DECIMAL(10, 8) NOT NULL,
    end_longitude DECIMAL(11, 8) NOT NULL,
    waypoints JSONB,

    -- AI predictions
    risk_score DECIMAL(5, 2),
    predicted_travel_time INTEGER, -- minutes
    alternative_routes JSONB,

    -- Personalization (MBTI, health, etc.)
    personalization_factors JSONB,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evacuation_user ON evacuation_plans(user_id);

-- AI prediction results
CREATE TABLE ai_predictions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,

    -- Prediction type
    prediction_type VARCHAR(50) NOT NULL, -- 'flood_risk', 'earthquake_risk', etc.

    -- Input data references
    environmental_data_ids UUID[],
    community_data_ids UUID[],
    health_data_ids UUID[],

    -- Prediction results
    risk_level VARCHAR(20), -- 'low', 'medium', 'high', 'critical'
    confidence_score DECIMAL(5, 2),
    predictions JSONB NOT NULL,
    recommendations JSONB,

    -- Model info
    model_version VARCHAR(50),

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_predictions_user ON ai_predictions(user_id);
CREATE INDEX idx_predictions_type ON ai_predictions(prediction_type);

-- ============================================
-- COLLABORATION
-- ============================================

-- Collaboration sessions
CREATE TABLE collaboration_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_name VARCHAR(255) NOT NULL,
    creator_id UUID REFERENCES users(id) ON DELETE SET NULL,

    -- Session details
    disaster_type VARCHAR(50),
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,

    -- Access control
    is_public BOOLEAN DEFAULT FALSE,
    password_hash VARCHAR(255),

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP
);

-- Session participants
CREATE TABLE session_participants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID REFERENCES collaboration_sessions(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    -- Participant details
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,

    UNIQUE(session_id, user_id)
);

CREATE INDEX idx_participants_session ON session_participants(session_id);

-- Session annotations
CREATE TABLE annotations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID REFERENCES collaboration_sessions(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,

    -- Annotation data
    annotation_type VARCHAR(50), -- 'marker', 'route', 'note', etc.
    content JSONB NOT NULL,

    -- Geographic reference
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_annotations_session ON annotations(session_id);

-- ============================================
-- LLM INTERACTIONS
-- ============================================

-- LLM queries and responses
CREATE TABLE llm_queries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,

    -- Query details
    query_text TEXT NOT NULL,
    context JSONB,
    mbti_type VARCHAR(4),

    -- Response
    response_text TEXT,
    response_metadata JSONB,

    -- Performance
    processing_time_ms INTEGER,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_llm_user ON llm_queries(user_id);

-- ============================================
-- SYSTEM MONITORING
-- ============================================

-- Resource usage monitoring
CREATE TABLE resource_usage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_name VARCHAR(100) NOT NULL,

    -- Metrics
    cpu_usage DECIMAL(5, 2),
    memory_usage_mb INTEGER,
    gpu_usage DECIMAL(5, 2),

    -- Additional metrics
    metrics JSONB,

    -- Timestamp
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resource_service ON resource_usage(service_name);
CREATE INDEX idx_resource_recorded ON resource_usage(recorded_at);

-- ============================================
-- FUNCTIONS & TRIGGERS
-- ============================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at trigger to relevant tables
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_env_data_updated_at BEFORE UPDATE ON environmental_data
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_community_data_updated_at BEFORE UPDATE ON community_data
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_health_data_updated_at BEFORE UPDATE ON individual_health_data
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to log audit events
CREATE OR REPLACE FUNCTION log_audit_event()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO audit_logs (user_id, action, resource_type, resource_id, details)
    VALUES (
        COALESCE(NEW.updated_by, NEW.created_by),
        TG_OP,
        TG_TABLE_NAME,
        NEW.id,
        row_to_json(NEW)
    );
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply audit logging to users table
CREATE TRIGGER audit_users AFTER INSERT OR UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION log_audit_event();

-- ============================================
-- INITIAL DATA
-- ============================================

-- Create default admin user (password: change_me_immediately)
-- Password hash for "change_me_immediately" using bcrypt
INSERT INTO users (username, email, password_hash, role, account_type, is_active, is_email_verified, first_name, last_name)
VALUES (
    'admin',
    'admin@disasterresiliencehub.org',
    '$2a$10$X5wFKgWQLs.KnLd3vYvCb.V6bXqZJ3Dn7qZtWJqYMxVPpQxnXG1Py',
    'ADMIN',
    'ENTERPRISE',
    TRUE,
    TRUE,
    'System',
    'Administrator'
);

-- Insert comment reminders
COMMENT ON TABLE users IS 'User accounts with support for standard auth, SSO (SAML/OAuth2/OIDC), MFA, and role-based access control (User/Moderator/Admin)';
COMMENT ON TABLE sso_providers IS 'SSO provider configurations for enterprise single sign-on integration';
COMMENT ON TABLE mfa_verifications IS 'Multi-factor authentication verification attempts and codes';
COMMENT ON TABLE audit_logs IS 'Security audit log for all critical user and system actions';
