import { useState } from 'react';
import { Link } from 'react-router-dom';
import LoginForm from '@components/auth/LoginForm';
import MFASetup from '@components/auth/MFASetup';

const Login = () => {
  const [showMFA, setShowMFA] = useState(false);
  const [mfaToken, setMfaToken] = useState(null);

  const handleLoginSuccess = (data) => {
    if (data.requires_mfa) {
      setMfaToken(data.mfa_token);
      setShowMFA(true);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold">
            Sign in to your account
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600 dark:text-gray-400">
            Or{' '}
            <Link
              to="/register"
              className="font-medium text-primary-600 hover:text-primary-500"
            >
              create a new account
            </Link>
          </p>
        </div>

        <div className="card">
          {showMFA ? (
            <MFASetup token={mfaToken} />
          ) : (
            <LoginForm onSuccess={handleLoginSuccess} />
          )}
        </div>

        <div className="text-center text-sm text-gray-600 dark:text-gray-400">
          <p>
            Forgot your password?{' '}
            <Link to="/reset-password" className="font-medium text-primary-600 hover:text-primary-500">
              Reset it here
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;
