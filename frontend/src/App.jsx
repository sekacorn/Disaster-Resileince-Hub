import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { AuthProvider } from '@/contexts/AuthContext';
import { ThemeProvider } from '@/contexts/ThemeContext';
import { useAuth } from '@hooks/useAuth';

// Pages
import Home from '@pages/Home';
import Login from '@pages/Login';
import Register from '@pages/Register';
import Dashboard from '@pages/Dashboard';
import DisasterMap from '@pages/DisasterMap';
import EvacuationPlanner from '@pages/EvacuationPlanner';
import Collaborate from '@pages/Collaborate';
import Profile from '@pages/Profile';
import DataUploadPage from '@pages/DataUploadPage';
import ChatPage from '@pages/ChatPage';

// Components
import Header from '@components/common/Header';
import Sidebar from '@components/common/Sidebar';
import Footer from '@components/common/Footer';
import SkipLink from '@components/a11y/SkipLink';
import RouteAnnouncer from '@components/a11y/RouteAnnouncer';

const PrivateRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      // The spinner is purely visual, so the loading state is also stated in text
      // for assistive technology and announced when it resolves (WCAG 4.1.3).
      <div
        className="flex items-center justify-center min-h-screen"
        role="status"
        aria-live="polite"
      >
        <div className="spinner" aria-hidden="true"></div>
        <span className="sr-only">Checking your sign-in status</span>
      </div>
    );
  }

  return isAuthenticated ? children : <Navigate to="/login" />;
};

const AppLayout = ({ children }) => {
  const { isAuthenticated } = useAuth();

  return (
    <div className="flex flex-col h-full">
      <SkipLink />
      <RouteAnnouncer />
      <Header />
      <div className="flex flex-1 overflow-hidden">
        {isAuthenticated && <Sidebar />}
        {/*
          id is the skip link target. tabIndex={-1} makes the landmark focusable by
          script without adding it to the tab order, so RouteAnnouncer can move focus
          here after a client-side navigation (WCAG 2.4.1, 2.4.3).
        */}
        <main id="main-content" tabIndex={-1} className="flex-1 overflow-auto">
          {children}
        </main>
      </div>
      <Footer />
    </div>
  );
};

function App() {
  return (
    <Router>
      <ThemeProvider>
        <AuthProvider>
          <AppLayout>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />

              <Route
                path="/dashboard"
                element={
                  <PrivateRoute>
                    <Dashboard />
                  </PrivateRoute>
                }
              />

              <Route
                path="/disaster-map"
                element={
                  <PrivateRoute>
                    <DisasterMap />
                  </PrivateRoute>
                }
              />

              <Route
                path="/evacuation-planner"
                element={
                  <PrivateRoute>
                    <EvacuationPlanner />
                  </PrivateRoute>
                }
              />

              <Route
                path="/collaborate"
                element={
                  <PrivateRoute>
                    <Collaborate />
                  </PrivateRoute>
                }
              />

              <Route
                path="/upload"
                element={
                  <PrivateRoute>
                    <DataUploadPage />
                  </PrivateRoute>
                }
              />

              <Route
                path="/chat"
                element={
                  <PrivateRoute>
                    <ChatPage />
                  </PrivateRoute>
                }
              />

              <Route
                path="/analytics"
                element={<Navigate to="/dashboard" />}
              />

              <Route
                path="/profile"
                element={
                  <PrivateRoute>
                    <Profile />
                  </PrivateRoute>
                }
              />

              <Route path="*" element={<Navigate to="/" />} />
            </Routes>
          </AppLayout>
          {/*
            WCAG 2.2.1 Timing Adjustable and 4.1.3 Status Messages.

            Five seconds is not enough time to notice, locate and read a message when
            using a screen magnifier or screen reader, and this application shows
            emergency information. The duration is raised well past the 20-second
            floor implied by 2.2.1, and pausing on hover or focus loss lets a reader
            hold it indefinitely. role="alert" puts each toast in the accessibility
            tree as an assertive live region so it is announced on arrival.
          */}
          <ToastContainer
            position="top-right"
            autoClose={20000}
            hideProgressBar={false}
            newestOnTop={true}
            closeOnClick
            rtl={false}
            pauseOnFocusLoss
            draggable
            pauseOnHover
            theme="colored"
            role="alert"
            closeButtonAriaLabel="Dismiss notification"
          />
        </AuthProvider>
      </ThemeProvider>
    </Router>
  );
}

export default App;
