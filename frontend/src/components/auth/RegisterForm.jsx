import { useState } from 'react';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { useAuth } from '@hooks/useAuth';
import { FaEnvelope, FaLock, FaUser, FaBuilding, FaEye, FaEyeSlash } from 'react-icons/fa';

const RegisterForm = () => {
  const { register } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const mbtiTypes = [
    'INTJ', 'INTP', 'ENTJ', 'ENTP',
    'INFJ', 'INFP', 'ENFJ', 'ENFP',
    'ISTJ', 'ISFJ', 'ESTJ', 'ESFJ',
    'ISTP', 'ISFP', 'ESTP', 'ESFP',
  ];

  const formik = useFormik({
    initialValues: {
      email: '',
      password: '',
      confirmPassword: '',
      full_name: '',
      organization: '',
      role: 'public_user',
      mbti_type: '',
    },
    validationSchema: Yup.object({
      email: Yup.string()
        .email('Invalid email address')
        .required('Email is required'),
      password: Yup.string()
        .min(8, 'Password must be at least 8 characters')
        .matches(/[A-Z]/, 'Password must contain at least one uppercase letter')
        .matches(/[a-z]/, 'Password must contain at least one lowercase letter')
        .matches(/[0-9]/, 'Password must contain at least one number')
        .required('Password is required'),
      confirmPassword: Yup.string()
        .oneOf([Yup.ref('password'), null], 'Passwords must match')
        .required('Please confirm your password'),
      full_name: Yup.string()
        .required('Full name is required'),
      organization: Yup.string(),
      role: Yup.string().required('Role is required'),
      mbti_type: Yup.string(),
    }),
    onSubmit: async (values) => {
      setLoading(true);
      const { confirmPassword, ...userData } = values;
      await register(userData);
      setLoading(false);
    },
  });

  return (
    <form onSubmit={formik.handleSubmit} className="space-y-4">
      {/* Full Name */}
      <div>
        <label htmlFor="full_name" className="block text-sm font-medium mb-2">
          Full Name
        </label>
        <div className="relative">
          <FaUser className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
          <input
            id="full_name"
            type="text"
            {...formik.getFieldProps('full_name')}
            className={`input pl-10 ${
              formik.touched.full_name && formik.errors.full_name
                ? 'border-danger-500'
                : ''
            }`}
            placeholder="John Doe"
          />
        </div>
        {formik.touched.full_name && formik.errors.full_name && (
          <div className="text-danger-500 text-sm mt-1">
            {formik.errors.full_name}
          </div>
        )}
      </div>

      {/* Email */}
      <div>
        <label htmlFor="email" className="block text-sm font-medium mb-2">
          Email Address
        </label>
        <div className="relative">
          <FaEnvelope className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
          <input
            id="email"
            type="email"
            {...formik.getFieldProps('email')}
            className={`input pl-10 ${
              formik.touched.email && formik.errors.email
                ? 'border-danger-500'
                : ''
            }`}
            placeholder="you@example.com"
          />
        </div>
        {formik.touched.email && formik.errors.email && (
          <div className="text-danger-500 text-sm mt-1">
            {formik.errors.email}
          </div>
        )}
      </div>

      {/* Password */}
      <div>
        <label htmlFor="password" className="block text-sm font-medium mb-2">
          Password
        </label>
        <div className="relative">
          <FaLock className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
          <input
            id="password"
            type={showPassword ? 'text' : 'password'}
            {...formik.getFieldProps('password')}
            className={`input pl-10 pr-10 ${
              formik.touched.password && formik.errors.password
                ? 'border-danger-500'
                : ''
            }`}
            placeholder="Enter password"
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
          >
            {showPassword ? <FaEyeSlash /> : <FaEye />}
          </button>
        </div>
        {formik.touched.password && formik.errors.password && (
          <div className="text-danger-500 text-sm mt-1">
            {formik.errors.password}
          </div>
        )}
      </div>

      {/* Confirm Password */}
      <div>
        <label htmlFor="confirmPassword" className="block text-sm font-medium mb-2">
          Confirm Password
        </label>
        <div className="relative">
          <FaLock className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
          <input
            id="confirmPassword"
            type="password"
            {...formik.getFieldProps('confirmPassword')}
            className={`input pl-10 ${
              formik.touched.confirmPassword && formik.errors.confirmPassword
                ? 'border-danger-500'
                : ''
            }`}
            placeholder="Confirm password"
          />
        </div>
        {formik.touched.confirmPassword && formik.errors.confirmPassword && (
          <div className="text-danger-500 text-sm mt-1">
            {formik.errors.confirmPassword}
          </div>
        )}
      </div>

      {/* Organization */}
      <div>
        <label htmlFor="organization" className="block text-sm font-medium mb-2">
          Organization (Optional)
        </label>
        <div className="relative">
          <FaBuilding className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
          <input
            id="organization"
            type="text"
            {...formik.getFieldProps('organization')}
            className="input pl-10"
            placeholder="Your organization"
          />
        </div>
      </div>

      {/* Role */}
      <div>
        <label htmlFor="role" className="block text-sm font-medium mb-2">
          Role
        </label>
        <select
          id="role"
          {...formik.getFieldProps('role')}
          className="input"
        >
          <option value="public_user">Public User</option>
          <option value="emergency_responder">Emergency Responder</option>
          <option value="government_official">Government Official</option>
          <option value="data_analyst">Data Analyst</option>
        </select>
      </div>

      {/* MBTI Type */}
      <div>
        <label htmlFor="mbti_type" className="block text-sm font-medium mb-2">
          MBTI Type (Optional)
        </label>
        <select
          id="mbti_type"
          {...formik.getFieldProps('mbti_type')}
          className="input"
        >
          <option value="">Select your MBTI type</option>
          {mbtiTypes.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
        <p className="text-xs text-gray-500 mt-1">
          This helps personalize your experience
        </p>
      </div>

      {/* Submit Button */}
      <button
        type="submit"
        disabled={loading || !formik.isValid}
        className="btn btn-primary w-full"
      >
        {loading ? 'Creating account...' : 'Create Account'}
      </button>
    </form>
  );
};

export default RegisterForm;
