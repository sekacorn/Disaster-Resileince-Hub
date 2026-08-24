import { useState, useEffect } from 'react';
import { useAuth } from '@hooks/useAuth';
import { userAPI, authAPI } from '@services/api';
import { toast } from 'react-toastify';
import { FaUser, FaShieldAlt, FaCog, FaHistory } from 'react-icons/fa';
import MFASetup from '@components/auth/MFASetup';

const Profile = () => {
  const { user, updateUser } = useAuth();
  const [activeTab, setActiveTab] = useState('profile');
  const [loading, setLoading] = useState(false);
  const [profileData, setProfileData] = useState({
    full_name: user?.full_name || '',
    email: user?.email || '',
    phone: user?.phone || '',
    organization: user?.organization || '',
  });
  const [activityLog, setActivityLog] = useState([]);
  const [showMFASetup, setShowMFASetup] = useState(false);

  useEffect(() => {
    if (activeTab === 'activity') {
      loadActivityLog();
    }
  }, [activeTab]);

  const loadActivityLog = async () => {
    try {
      const response = await userAPI.getActivityLog();
      setActivityLog(response.data.activities || []);
    } catch (error) {
      toast.error('Failed to load activity log');
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setProfileData({ ...profileData, [name]: value });
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await userAPI.updateProfile(profileData);
      updateUser(response.data);
      toast.success('Profile updated successfully');
    } catch (error) {
      toast.error('Failed to update profile');
    } finally {
      setLoading(false);
    }
  };

  const handleEnableMFA = () => {
    setShowMFASetup(true);
  };

  const handleDisableMFA = async () => {
    if (!confirm('Are you sure you want to disable MFA?')) return;

    try {
      await authAPI.disableMFA();
      updateUser({ ...user, mfa_enabled: false });
      toast.success('MFA disabled successfully');
    } catch (error) {
      toast.error('Failed to disable MFA');
    }
  };

  const tabs = [
    { id: 'profile', label: 'Profile', icon: <FaUser /> },
    { id: 'security', label: 'Security', icon: <FaShieldAlt /> },
    { id: 'preferences', label: 'Preferences', icon: <FaCog /> },
    { id: 'activity', label: 'Activity', icon: <FaHistory /> },
  ];

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">Profile Settings</h1>

      {/* Tabs */}
      <div className="border-b border-gray-200 dark:border-gray-700 mb-6">
        <div className="flex space-x-8">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`pb-4 px-1 flex items-center gap-2 border-b-2 transition-colors ${
                activeTab === tab.id
                  ? 'border-primary-600 text-primary-600 dark:text-primary-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
              }`}
            >
              {tab.icon}
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Tab Content */}
      <div className="space-y-6">
        {/* Profile Tab */}
        {activeTab === 'profile' && (
          <div className="card">
            <h2 className="text-xl font-bold mb-4">Personal Information</h2>
            <form onSubmit={handleUpdateProfile} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="profile-full-name" className="block text-sm font-medium mb-2">
                    Full Name
                  </label>
                  <input
                    id="profile-full-name"
                    autoComplete="name"
                    type="text"
                    name="full_name"
                    value={profileData.full_name}
                    onChange={handleInputChange}
                    className="input"
                  />
                </div>
                <div>
                  <label htmlFor="profile-email" className="block text-sm font-medium mb-2">
                    Email
                  </label>
                  <input
                    id="profile-email"
                    autoComplete="email"
                    type="email"
                    name="email"
                    value={profileData.email}
                    onChange={handleInputChange}
                    className="input"
                  />
                </div>
                <div>
                  <label htmlFor="profile-phone" className="block text-sm font-medium mb-2">
                    Phone
                  </label>
                  <input
                    id="profile-phone"
                    autoComplete="tel"
                    type="tel"
                    name="phone"
                    value={profileData.phone}
                    onChange={handleInputChange}
                    className="input"
                  />
                </div>
                <div>
                  <label htmlFor="profile-organization" className="block text-sm font-medium mb-2">
                    Organization
                  </label>
                  <input
                    id="profile-organization"
                    autoComplete="organization"
                    type="text"
                    name="organization"
                    value={profileData.organization}
                    onChange={handleInputChange}
                    className="input"
                  />
                </div>
                <div>
                  <label htmlFor="profile-role" className="block text-sm font-medium mb-2">
                    Role
                  </label>
                  {/*
                    readOnly rather than disabled: a disabled input is skipped by the
                    tab order and is not reliably announced, so a screen reader user
                    never learns their own role. readOnly stays focusable and readable
                    while remaining uneditable.
                  */}
                  <input
                    id="profile-role"
                    type="text"
                    value={user?.role || 'N/A'}
                    className="input"
                    readOnly
                    aria-readonly="true"
                  />
                </div>
              </div>
              <button
                type="submit"
                disabled={loading}
                className="btn btn-primary"
              >
                {loading ? 'Saving...' : 'Save Changes'}
              </button>
            </form>
          </div>
        )}

        {/* Security Tab */}
        {activeTab === 'security' && (
          <div className="space-y-4">
            <div className="card">
              <h2 className="text-xl font-bold mb-4">Two-Factor Authentication</h2>
              {user?.mfa_enabled ? (
                <div>
                  <div className="alert alert-success mb-4">
                    MFA is currently enabled on your account
                  </div>
                  <button
                    onClick={handleDisableMFA}
                    className="btn btn-danger"
                  >
                    Disable MFA
                  </button>
                </div>
              ) : showMFASetup ? (
                <MFASetup onComplete={() => setShowMFASetup(false)} />
              ) : (
                <div>
                  <div className="alert alert-warning mb-4">
                    MFA is not enabled. We recommend enabling it for better security.
                  </div>
                  <button
                    onClick={handleEnableMFA}
                    className="btn btn-primary"
                  >
                    Enable MFA
                  </button>
                </div>
              )}
            </div>

            <div className="card">
              <h2 className="text-xl font-bold mb-4">Change Password</h2>
              <form className="space-y-4">
                <div>
                  <label htmlFor="current-password" className="block text-sm font-medium mb-2">
                    Current Password
                  </label>
                  <input
                    id="current-password"
                    type="password"
                    autoComplete="current-password"
                    className="input"
                  />
                </div>
                <div>
                  <label htmlFor="new-password" className="block text-sm font-medium mb-2">
                    New Password
                  </label>
                  <input
                    id="new-password"
                    type="password"
                    autoComplete="new-password"
                    className="input"
                  />
                </div>
                <div>
                  <label htmlFor="confirm-new-password" className="block text-sm font-medium mb-2">
                    Confirm New Password
                  </label>
                  <input
                    id="confirm-new-password"
                    type="password"
                    autoComplete="new-password"
                    className="input"
                  />
                </div>
                <button type="submit" className="btn btn-primary">
                  Update Password
                </button>
              </form>
            </div>
          </div>
        )}

        {/* Preferences Tab */}
        {activeTab === 'preferences' && (
          <div className="card">
            <h2 className="text-xl font-bold mb-4">Application Preferences</h2>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  {/*
                    Was a bare <div>, so the toggle beside it had no accessible name at
                    all. A real <label> binds this text to the checkbox, and the
                    description is referenced so it is read as well (WCAG 1.3.1, 3.3.2).
                  */}
                  <label htmlFor="pref-email-notifications" className="font-medium">
                    Email Notifications
                  </label>
                  <div id="pref-email-notifications-description" className="text-sm text-gray-600 dark:text-gray-400">
                    Receive email alerts for disasters
                  </div>
                </div>
                <div className="relative inline-flex items-center">
                  <input
                    id="pref-email-notifications"
                    type="checkbox"
                    role="switch"
                    aria-describedby="pref-email-notifications-description"
                    className="sr-only peer"
                    defaultChecked
                  />
                  {/*
                    The visible track is decorative; state comes from the checkbox
                    itself, which stays in the accessibility tree via sr-only rather
                    than display:none.
                  */}
                  <label htmlFor="pref-email-notifications" className="cursor-pointer">
                    <span className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 dark:peer-focus:ring-primary-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-primary-600"></span>
                    <span className="sr-only">Email Notifications</span>
                  </label>
                </div>
              </div>

              <div className="flex items-center justify-between">
                <div>
                  {/*
                    Was a bare <div>, so the toggle beside it had no accessible name at
                    all. A real <label> binds this text to the checkbox, and the
                    description is referenced so it is read as well (WCAG 1.3.1, 3.3.2).
                  */}
                  <label htmlFor="pref-push-notifications" className="font-medium">
                    Push Notifications
                  </label>
                  <div id="pref-push-notifications-description" className="text-sm text-gray-600 dark:text-gray-400">
                    Receive browser notifications
                  </div>
                </div>
                <div className="relative inline-flex items-center">
                  <input
                    id="pref-push-notifications"
                    type="checkbox"
                    role="switch"
                    aria-describedby="pref-push-notifications-description"
                    className="sr-only peer"
                    defaultChecked
                  />
                  {/*
                    The visible track is decorative; state comes from the checkbox
                    itself, which stays in the accessibility tree via sr-only rather
                    than display:none.
                  */}
                  <label htmlFor="pref-push-notifications" className="cursor-pointer">
                    <span className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 dark:peer-focus:ring-primary-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-primary-600"></span>
                    <span className="sr-only">Push Notifications</span>
                  </label>
                </div>
              </div>

              <div className="flex items-center justify-between">
                <div>
                  {/*
                    Was a bare <div>, so the toggle beside it had no accessible name at
                    all. A real <label> binds this text to the checkbox, and the
                    description is referenced so it is read as well (WCAG 1.3.1, 3.3.2).
                  */}
                  <label htmlFor="pref-auto-save" className="font-medium">
                    Auto-save
                  </label>
                  <div id="pref-auto-save-description" className="text-sm text-gray-600 dark:text-gray-400">
                    Automatically save your work
                  </div>
                </div>
                <div className="relative inline-flex items-center">
                  <input
                    id="pref-auto-save"
                    type="checkbox"
                    role="switch"
                    aria-describedby="pref-auto-save-description"
                    className="sr-only peer"
                    defaultChecked
                  />
                  {/*
                    The visible track is decorative; state comes from the checkbox
                    itself, which stays in the accessibility tree via sr-only rather
                    than display:none.
                  */}
                  <label htmlFor="pref-auto-save" className="cursor-pointer">
                    <span className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 dark:peer-focus:ring-primary-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-primary-600"></span>
                    <span className="sr-only">Auto-save</span>
                  </label>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Activity Tab */}
        {activeTab === 'activity' && (
          <div className="card">
            <h2 className="text-xl font-bold mb-4">Recent Activity</h2>
            {activityLog.length > 0 ? (
              <div className="space-y-3">
                {activityLog.map((activity, index) => (
                  <div
                    key={index}
                    className="flex items-start gap-3 pb-3 border-b border-gray-200 dark:border-gray-700 last:border-0"
                  >
                    <div className="flex-1">
                      <div className="font-medium">{activity.action}</div>
                      <div className="text-sm text-gray-600 dark:text-gray-400">
                        {activity.description}
                      </div>
                      <div className="text-xs text-gray-500 mt-1">
                        {new Date(activity.timestamp).toLocaleString()}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-8 text-gray-500">
                No recent activity
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default Profile;
