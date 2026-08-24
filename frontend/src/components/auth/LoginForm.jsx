import { useState } from 'react';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { useAuth } from '@hooks/useAuth';
import { FaEnvelope, FaLock, FaEye, FaEyeSlash } from 'react-icons/fa';
import FormField from '@components/a11y/FormField';

const LoginForm = ({ onSuccess }) => {
  const { login } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [submitError, setSubmitError] = useState('');

  const formik = useFormik({
    initialValues: {
      email: '',
      password: '',
      remember: false,
    },
    validationSchema: Yup.object({
      email: Yup.string()
        .email('Enter an email address in the format name@example.com')
        .required('Enter your email address'),
      password: Yup.string().required('Enter your password'),
    }),
    onSubmit: async (values) => {
      setLoading(true);
      setSubmitError('');
      const result = await login(values.email, values.password);
      setLoading(false);

      if (result.success) {
        if (onSuccess) {
          onSuccess(result.data);
        }
        return;
      }
      // Surfaced in the form rather than only as a toast: a toast can time out
      // before it is found, and the error belongs next to the controls it concerns.
      setSubmitError(
        result.error || 'We could not sign you in. Check your details and try again.'
      );
    },
  });

  return (
    <form onSubmit={formik.handleSubmit} className="space-y-4" noValidate>
      {/*
        Form-level failure, announced assertively because the user has just acted and
        is waiting on the outcome (WCAG 3.3.1, 4.1.3).
      */}
      {submitError && (
        <div className="alert alert-danger" role="alert">
          {submitError}
        </div>
      )}

      <FormField
        label="Email Address"
        required
        error={formik.errors.email}
        touched={formik.touched.email}
      >
        {(fieldProps) => (
          <div className="relative">
            <FaEnvelope
              className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-500 dark:text-gray-400"
              aria-hidden="true"
            />
            <input
              {...fieldProps}
              type="email"
              // Lets a browser or password manager fill the field, which WCAG 1.3.5
              // Identify Input Purpose requires and which reduces typing for anyone
              // with a motor impairment.
              autoComplete="username"
              {...formik.getFieldProps('email')}
              className={`input pl-10 ${
                formik.touched.email && formik.errors.email ? 'border-danger-500' : ''
              }`}
              placeholder="you@example.com"
            />
          </div>
        )}
      </FormField>

      <FormField
        label="Password"
        required
        error={formik.errors.password}
        touched={formik.touched.password}
      >
        {(fieldProps) => (
          <div className="relative">
            <FaLock
              className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-500 dark:text-gray-400"
              aria-hidden="true"
            />
            <input
              {...fieldProps}
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              {...formik.getFieldProps('password')}
              className={`input pl-10 pr-10 ${
                formik.touched.password && formik.errors.password
                  ? 'border-danger-500'
                  : ''
              }`}
              placeholder="Enter your password"
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
              // Was an unlabelled icon button, so it was announced only as "button".
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              aria-pressed={showPassword}
            >
              {showPassword ? (
                <FaEyeSlash aria-hidden="true" />
              ) : (
                <FaEye aria-hidden="true" />
              )}
            </button>
          </div>
        )}
      </FormField>

      {/* Remember Me */}
      <div className="flex items-center">
        <input
          id="remember"
          type="checkbox"
          {...formik.getFieldProps('remember')}
          className="w-4 h-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
        />
        <label htmlFor="remember" className="ml-2 text-sm">
          Remember me
        </label>
      </div>

      {/*
        The button stays enabled even when the form is invalid. Disabling it removes
        it from the tab order and gives no reason for the block, leaving a keyboard
        user at a dead end; submitting instead surfaces the specific field errors,
        which is what 3.3.1 Error Identification asks for.
      */}
      <button type="submit" disabled={loading} className="btn btn-primary w-full">
        {loading ? 'Signing in…' : 'Sign In'}
      </button>

      {/* Announces the in-flight state, which the button label alone does not. */}
      <div role="status" aria-live="polite" className="sr-only">
        {loading ? 'Signing in, please wait' : ''}
      </div>
    </form>
  );
};

export default LoginForm;
