import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { StatusBadge } from './components/common/StatusBadge';
import { LoginPage } from './pages/auth/LoginPage';
import { AuthProvider } from './context/AuthContext';

describe('Common Components', () => {
  it('renders StatusBadge with correct formatting', () => {
    render(<StatusBadge status="APPROVED" />);
    expect(screen.getByText('APPROVED')).toBeInTheDocument();

    render(<StatusBadge status="IN_PROGRESS" />);
    expect(screen.getByText('IN PROGRESS')).toBeInTheDocument();
  });
});

describe('Authentication Flow', () => {
  it('renders Login page with quick login buttons for all 3 roles', () => {
    render(
      <BrowserRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </BrowserRouter>
    );

    expect(screen.getByText('ExamForge')).toBeInTheDocument();
    expect(screen.getByText('College Management')).toBeInTheDocument();
    expect(screen.getByText(/Dr\. Rajesh Sharma \(Faculty\)/i)).toBeInTheDocument();
    expect(screen.getByText(/Alex Johnson \(Student\)/i)).toBeInTheDocument();
  });
});
