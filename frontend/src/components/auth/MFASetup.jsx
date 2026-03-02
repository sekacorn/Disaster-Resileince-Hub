import { useState, useEffect } from 'react';
import { authAPI } from '@services/api';
import { useAuth } from '@hooks/useAuth';
import { toast } from 'react-toastify';
import { QRCodeSVG } from 'qrcode.react';
import { FaShieldAlt } from 'react-icons/fa';

const MFASetup = ({ token, onComplete }) => {
  const { updateUser } = useAuth();
  const [qrCode, setQrCode] = useState(null);
  const [secret, setSecret] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [step, setStep] = useState(1);

  useEffect(() => {
    setupMFA();
  }, []);

  const setupMFA = async () => {
    try {
      const response = await authAPI.setupMFA();
      setQrCode(response.data.qr_code);
      setSecret(response.data.secret);
    } catch (error) {
      toast.error('Failed to setup MFA');
    }
  };

  const handleVerify = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      await authAPI.verifyMFA(verificationCode);
      updateUser({ mfa_enabled: true });
      toast.success('MFA enabled successfully');
      if (onComplete) {
        onComplete();
      }
    } catch (error) {
      toast.error('Invalid verification code');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="text-center">
        <div className="inline-flex items-center justify-center w-16 h-16 bg-primary-100 dark:bg-primary-900 rounded-full mb-4">
          <FaShieldAlt className="w-8 h-8 text-primary-600 dark:text-primary-400" />
        </div>
        <h3 className="text-xl font-bold mb-2">
          Two-Factor Authentication Setup
        </h3>
        <p className="text-sm text-gray-600 dark:text-gray-400">
          Secure your account with an additional layer of protection
        </p>
      </div>

      {step === 1 && qrCode && (
        <div className="space-y-4">
          <div className="alert alert-info">
            <p className="text-sm">
              Scan this QR code with your authenticator app (Google Authenticator, Authy, etc.)
            </p>
          </div>

          <div className="flex justify-center p-4 bg-white rounded-lg">
            <QRCodeSVG value={qrCode} size={200} />
          </div>

          <div className="bg-gray-50 dark:bg-gray-900 p-4 rounded-lg">
            <p className="text-xs text-gray-600 dark:text-gray-400 mb-2">
              Manual entry code:
            </p>
            <code className="text-sm font-mono break-all">{secret}</code>
          </div>

          <button
            onClick={() => setStep(2)}
            className="btn btn-primary w-full"
          >
            I've Scanned the Code
          </button>
        </div>
      )}

      {step === 2 && (
        <form onSubmit={handleVerify} className="space-y-4">
          <div className="alert alert-info">
            <p className="text-sm">
              Enter the 6-digit code from your authenticator app
            </p>
          </div>

          <div>
            <label htmlFor="code" className="block text-sm font-medium mb-2">
              Verification Code
            </label>
            <input
              id="code"
              type="text"
              value={verificationCode}
              onChange={(e) => setVerificationCode(e.target.value)}
              className="input text-center text-2xl tracking-widest"
              placeholder="000000"
              maxLength={6}
              pattern="\d{6}"
              required
            />
          </div>

          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => setStep(1)}
              className="btn btn-secondary flex-1"
            >
              Back
            </button>
            <button
              type="submit"
              disabled={loading || verificationCode.length !== 6}
              className="btn btn-primary flex-1"
            >
              {loading ? 'Verifying...' : 'Verify & Enable'}
            </button>
          </div>
        </form>
      )}
    </div>
  );
};

export default MFASetup;
