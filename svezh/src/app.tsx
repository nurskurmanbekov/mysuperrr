import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './hooks/useAuth';
import Login from './components/auth/login';
import Dashboard from './components/dashboard/dashboard';
import Registry from './components/registry/Registry';
import Devices from './components/devices/Devices';
import Events from './components/events/events';
import Layout from './components/common/layout';
import AdminPanel from './components/admin/AdminPanel';
import GeoZoneManager from './components/geozones/GeoZoneManager';
import TrackPlayback from './components/playback/TrackPlayback';

// Импортируем карту с ленивой загрузкой
const RealMap = React.lazy(() => import('./components/map/RealMap'));

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, loading } = useAuth();
  
  if (loading) return <div>Загрузка...</div>;
  return user ? <>{children}</> : <Navigate to="/login" />;
};

const App: React.FC = () => {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={
            <ProtectedRoute>
              <Layout>
                <Dashboard />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/registry" element={
            <ProtectedRoute>
              <Layout>
                <Registry />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/devices" element={
            <ProtectedRoute>
              <Layout>
                <Devices />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/events" element={
            <ProtectedRoute>
              <Layout>
                <Events />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/map" element={
            <ProtectedRoute>
              <Layout>
                <React.Suspense fallback={<div>Загрузка карты...</div>}>
                  <RealMap />
                </React.Suspense>
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/admin" element={
            <ProtectedRoute>
              <Layout>
                <AdminPanel />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/geozones" element={
            <ProtectedRoute>
              <Layout>
                <GeoZoneManager />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/playback" element={
            <ProtectedRoute>
              <Layout>
                <TrackPlayback />
              </Layout>
            </ProtectedRoute>
          } />
        </Routes>
      </Router>
    </AuthProvider>
  );
};

export default App;