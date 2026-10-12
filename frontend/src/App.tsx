import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { ProtectedRoute } from './components/auth/ProtectedRoute';
import { Layout } from './components/common/Layout';

// Auth Pages
import { LoginPage } from './pages/auth/LoginPage';

// Management Pages
import { ManagementDashboard } from './pages/management/ManagementDashboard';
import { UserManagement } from './pages/management/UserManagement';
import { AcademicManagement } from './pages/management/AcademicManagement';
import { ExamSchedulingPage } from './pages/management/ExamSchedulingPage';
import { AuditLogsPage } from './pages/management/AuditLogsPage';

// Teacher Pages
import { TeacherDashboard } from './pages/teacher/TeacherDashboard';
import { DocumentLibraryPage } from './pages/teacher/DocumentLibraryPage';
import { BlueprintBuilderPage } from './pages/teacher/BlueprintBuilderPage';
import { AiQuestionGeneratorPage } from './pages/teacher/AiQuestionGeneratorPage';
import { PaperReviewEditorPage } from './pages/teacher/PaperReviewEditorPage';
import { TeacherAnalyticsPage } from './pages/teacher/TeacherAnalyticsPage';

// Student Pages
import { StudentDashboard } from './pages/student/StudentDashboard';
import { ExamInstructionsPage } from './pages/student/ExamInstructionsPage';
import { TimedExamRoom } from './pages/student/TimedExamRoom';
import { StudentResultView } from './pages/student/StudentResultView';

// Common Pages
import { NotFoundPage } from './pages/common/NotFoundPage';
import { AccessDeniedPage } from './pages/common/AccessDeniedPage';

const RoleBasedRedirect: React.FC = () => {
  const { user, isAuthenticated } = useAuth();

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  if (user.role === 'MANAGEMENT') {
    return <Navigate to="/management" replace />;
  }
  if (user.role === 'TEACHER') {
    return <Navigate to="/teacher" replace />;
  }
  if (user.role === 'STUDENT') {
    return <Navigate to="/student" replace />;
  }

  return <Navigate to="/login" replace />;
};

export const App: React.FC = () => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/403" element={<AccessDeniedPage />} />
      <Route path="/access-denied" element={<AccessDeniedPage />} />

      {/* Root redirect based on role */}
      <Route path="/" element={<RoleBasedRedirect />} />

      {/* Protected Management Routes */}
      <Route
        path="/management"
        element={
          <ProtectedRoute allowedRoles={['MANAGEMENT']}>
            <Layout>
              <ManagementDashboard />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/management/users"
        element={
          <ProtectedRoute allowedRoles={['MANAGEMENT']}>
            <Layout>
              <UserManagement />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/management/academic"
        element={
          <ProtectedRoute allowedRoles={['MANAGEMENT']}>
            <Layout>
              <AcademicManagement />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/management/schedules"
        element={
          <ProtectedRoute allowedRoles={['MANAGEMENT']}>
            <Layout>
              <ExamSchedulingPage />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/management/audit-logs"
        element={
          <ProtectedRoute allowedRoles={['MANAGEMENT']}>
            <Layout>
              <AuditLogsPage />
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* Protected Teacher Routes */}
      <Route
        path="/teacher"
        element={
          <ProtectedRoute allowedRoles={['TEACHER']}>
            <Layout>
              <TeacherDashboard />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/teacher/documents"
        element={
          <ProtectedRoute allowedRoles={['TEACHER']}>
            <Layout>
              <DocumentLibraryPage />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/teacher/blueprints"
        element={
          <ProtectedRoute allowedRoles={['TEACHER']}>
            <Layout>
              <BlueprintBuilderPage />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/teacher/ai-generate"
        element={
          <ProtectedRoute allowedRoles={['TEACHER']}>
            <Layout>
              <AiQuestionGeneratorPage />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/teacher/papers/:paperId"
        element={
          <ProtectedRoute allowedRoles={['TEACHER']}>
            <Layout>
              <PaperReviewEditorPage />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/teacher/analytics"
        element={
          <ProtectedRoute allowedRoles={['TEACHER']}>
            <Layout>
              <TeacherAnalyticsPage />
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* Protected Student Routes */}
      <Route
        path="/student"
        element={
          <ProtectedRoute allowedRoles={['STUDENT']}>
            <Layout>
              <StudentDashboard />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/student/exams/:scheduleId/instructions"
        element={
          <ProtectedRoute allowedRoles={['STUDENT']}>
            <Layout>
              <ExamInstructionsPage />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/student/exams/:scheduleId/room"
        element={
          <ProtectedRoute allowedRoles={['STUDENT']}>
            <Layout>
              <TimedExamRoom />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/student/results/:attemptId"
        element={
          <ProtectedRoute allowedRoles={['STUDENT']}>
            <Layout>
              <StudentResultView />
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* 404 Catch-All */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
};

export default App;
