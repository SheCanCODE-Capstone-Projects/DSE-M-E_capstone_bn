# Email Verification Protocol - Frontend Implementation Guide

## How the Email Verification System Works

### Backend Flow:
1. **User Registers** → Backend creates user with `isVerified: false`
2. **Token Generated** → UUID token created, expires in 24 hours
3. **Email Sent** → Link sent to user: `http://localhost:3000/verify?token={UUID}`
4. **User Clicks Link** → Frontend calls backend verification endpoint
5. **Backend Verifies** → Sets `isVerified: true`, deletes token
6. **User Can Login** → Only verified users can login

---

## Frontend Implementation

### 1. Registration Flow

**Registration Component:**
```javascript
// Registration.jsx
import { useState } from 'react';
import { toast } from 'react-toastify';

const Registration = () => {
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: ''
  });
  const [isLoading, setIsLoading] = useState(false);
  const [showVerificationMessage, setShowVerificationMessage] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const response = await fetch('http://localhost:8088/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData)
      });

      const message = await response.text();

      if (response.ok) {
        setShowVerificationMessage(true);
        toast.success('Registration successful! Check your email.');
      } else {
        toast.error(message || 'Registration failed');
      }
    } catch (error) {
      toast.error('Network error. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  if (showVerificationMessage) {
    return <VerificationPending email={formData.email} />;
  }

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="email"
        placeholder="Email"
        value={formData.email}
        onChange={(e) => setFormData({...formData, email: e.target.value})}
        required
      />
      <input
        type="password"
        placeholder="Password (5-20 characters)"
        value={formData.password}
        onChange={(e) => setFormData({...formData, password: e.target.value})}
        minLength={5}
        maxLength={20}
        required
      />
      <input
        type="text"
        placeholder="First Name"
        value={formData.firstName}
        onChange={(e) => setFormData({...formData, firstName: e.target.value})}
      />
      <input
        type="text"
        placeholder="Last Name"
        value={formData.lastName}
        onChange={(e) => setFormData({...formData, lastName: e.target.value})}
      />
      <button type="submit" disabled={isLoading}>
        {isLoading ? 'Registering...' : 'Register'}
      </button>
    </form>
  );
};
```

### 2. Verification Pending Component

```javascript
// VerificationPending.jsx
import { useState } from 'react';
import { toast } from 'react-toastify';

const VerificationPending = ({ email }) => {
  const [isResending, setIsResending] = useState(false);
  const [canResend, setCanResend] = useState(true);

  const handleResendEmail = async () => {
    if (!canResend) return;
    
    setIsResending(true);
    setCanResend(false);

    try {
      const response = await fetch(`http://localhost:8088/api/auth/resend-verification?email=${email}`, {
        method: 'POST'
      });

      const message = await response.text();

      if (response.ok) {
        toast.success('Verification email sent!');
        // Rate limit: disable resend for 5 minutes
        setTimeout(() => setCanResend(true), 5 * 60 * 1000);
      } else {
        toast.error(message || 'Failed to resend email');
        setCanResend(true);
      }
    } catch (error) {
      toast.error('Network error. Please try again.');
      setCanResend(true);
    } finally {
      setIsResending(false);
    }
  };

  return (
    <div className="verification-pending">
      <h2>Check Your Email</h2>
      <p>We've sent a verification link to <strong>{email}</strong></p>
      <p>Click the link in the email to verify your account.</p>
      
      <div className="resend-section">
        <p>Didn't receive the email?</p>
        <button 
          onClick={handleResendEmail}
          disabled={!canResend || isResending}
        >
          {isResending ? 'Sending...' : 'Resend Email'}
        </button>
        {!canResend && <p>Please wait 5 minutes before resending</p>}
      </div>

      <div className="tips">
        <h4>Tips:</h4>
        <ul>
          <li>Check your spam/junk folder</li>
          <li>Make sure {email} is correct</li>
          <li>The link expires in 24 hours</li>
        </ul>
      </div>
    </div>
  );
};
```

### 3. Email Verification Handler

```javascript
// EmailVerification.jsx
import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';

const EmailVerification = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('verifying'); // 'verifying', 'success', 'error'
  const [message, setMessage] = useState('');

  useEffect(() => {
    const token = searchParams.get('token');
    
    if (!token) {
      setStatus('error');
      setMessage('Invalid verification link');
      return;
    }

    verifyEmail(token);
  }, [searchParams]);

  const verifyEmail = async (token) => {
    try {
      const response = await fetch(`http://localhost:8088/api/auth/verify?token=${token}`);
      const message = await response.text();

      if (response.ok) {
        setStatus('success');
        setMessage('Email verified successfully!');
        toast.success('Email verified! You can now login.');
        
        // Redirect to login after 3 seconds
        setTimeout(() => {
          navigate('/login');
        }, 3000);
      } else {
        setStatus('error');
        setMessage(message || 'Verification failed');
        toast.error(message || 'Verification failed');
      }
    } catch (error) {
      setStatus('error');
      setMessage('Network error occurred');
      toast.error('Network error occurred');
    }
  };

  return (
    <div className="email-verification">
      {status === 'verifying' && (
        <div>
          <h2>Verifying Email...</h2>
          <div className="spinner">Loading...</div>
        </div>
      )}

      {status === 'success' && (
        <div className="success">
          <h2>✅ Email Verified!</h2>
          <p>{message}</p>
          <p>Redirecting to login...</p>
          <button onClick={() => navigate('/login')}>
            Go to Login Now
          </button>
        </div>
      )}

      {status === 'error' && (
        <div className="error">
          <h2>❌ Verification Failed</h2>
          <p>{message}</p>
          <div className="actions">
            <button onClick={() => navigate('/register')}>
              Register Again
            </button>
            <button onClick={() => navigate('/login')}>
              Try Login
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
```

### 4. Updated Login Component

```javascript
// Login.jsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';

const Login = () => {
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const response = await fetch('http://localhost:8088/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData)
      });

      const result = await response.text();

      if (response.ok) {
        // Check if it's a JSON response (for UNASSIGNED users)
        try {
          const jsonResult = JSON.parse(result);
          if (jsonResult.redirectTo) {
            localStorage.setItem('token', jsonResult.token);
            toast.info(jsonResult.message);
            navigate(jsonResult.redirectTo);
            return;
          }
        } catch {
          // It's a plain token string
          localStorage.setItem('token', result);
          toast.success('Login successful!');
          navigate('/dashboard');
        }
      } else {
        if (result.includes('verify your email')) {
          toast.error('Please verify your email first');
          navigate('/verification-pending', { state: { email: formData.email } });
        } else {
          toast.error(result || 'Login failed');
        }
      }
    } catch (error) {
      toast.error('Network error. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="email"
        placeholder="Email"
        value={formData.email}
        onChange={(e) => setFormData({...formData, email: e.target.value})}
        required
      />
      <input
        type="password"
        placeholder="Password"
        value={formData.password}
        onChange={(e) => setFormData({...formData, password: e.target.value})}
        required
      />
      <button type="submit" disabled={isLoading}>
        {isLoading ? 'Logging in...' : 'Login'}
      </button>
    </form>
  );
};
```

### 5. Router Setup

```javascript
// App.jsx or Router setup
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/register" element={<Registration />} />
        <Route path="/login" element={<Login />} />
        <Route path="/verify" element={<EmailVerification />} />
        <Route path="/verification-pending" element={<VerificationPending />} />
        {/* Other routes */}
      </Routes>
      
      <ToastContainer
        position="top-right"
        autoClose={5000}
        hideProgressBar={false}
        newestOnTop={false}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
      />
    </BrowserRouter>
  );
}
```

---

## Key Points for Frontend Implementation:

### 1. **Email Link Format**
The backend sends: `http://localhost:3000/verify?token={UUID}`
- Change `localhost:3000` to your frontend URL
- Token is a UUID string
- Link expires in 24 hours

### 2. **API Endpoints to Use**
```javascript
// Registration
POST http://localhost:8088/api/auth/register

// Email Verification
GET http://localhost:8088/api/auth/verify?token={token}

// Resend Verification
POST http://localhost:8088/api/auth/resend-verification?email={email}

// Login
POST http://localhost:8088/api/auth/login
```

### 3. **Error Handling**
- **Email already exists**: Show error, suggest login
- **Invalid token**: Show error, offer to resend
- **Expired token**: Show error, offer to resend
- **Network errors**: Show retry option

### 4. **Rate Limiting**
- Resend email: 5-minute cooldown
- Show countdown timer to user
- Disable resend button during cooldown

### 5. **User Experience**
- Clear success/error messages
- Loading states for all actions
- Automatic redirects after verification
- Help text and tips for users

### 6. **Security Considerations**
- Never expose tokens in logs
- Use HTTPS in production
- Validate all user inputs
- Handle expired tokens gracefully

### 7. **Testing Checklist**
- [ ] Registration with valid email
- [ ] Registration with existing email
- [ ] Email verification with valid token
- [ ] Email verification with invalid token
- [ ] Email verification with expired token
- [ ] Resend verification email
- [ ] Rate limiting on resend
- [ ] Login before verification
- [ ] Login after verification

This implementation provides a complete email verification flow that matches your backend protocol!