import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  Users,
  GraduationCap,
  Calendar,
  FileText,
  Sparkles,
  ClipboardCheck,
  BarChart3,
  BookOpen,
  FolderOpen,
  FileCheck2,
  History,
  ShieldCheck,
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const { user } = useAuth();
  if (!user) return null;

  const managementNav = [
    { label: 'Overview', to: '/management', icon: LayoutDashboard },
    { label: 'User Directory', to: '/management/users', icon: Users },
    { label: 'Academic Structure', to: '/management/academic', icon: GraduationCap },
    { label: 'Exam Scheduling', to: '/management/schedules', icon: Calendar },
    { label: 'Audit Trail', to: '/management/audit-logs', icon: ShieldCheck },
  ];

  const teacherNav = [
    { label: 'Faculty Dashboard', to: '/teacher', icon: LayoutDashboard },
    { label: 'Lesson Materials', to: '/teacher/documents', icon: FolderOpen },
    { label: 'Exam Blueprints', to: '/teacher/blueprints', icon: FileText },
    { label: 'AI Question Generator', to: '/teacher/generator', icon: Sparkles },
    { label: 'Paper Review & Approval', to: '/teacher/papers', icon: ClipboardCheck },
    { label: 'Class Analytics', to: '/teacher/analytics', icon: BarChart3 },
  ];

  const studentNav = [
    { label: 'Student Dashboard', to: '/student', icon: LayoutDashboard },
    { label: 'Available Exams', to: '/student/exams', icon: Calendar },
    { label: 'Results & History', to: '/student/results', icon: History },
  ];

  const navItems =
    user.role === 'MANAGEMENT'
      ? managementNav
      : user.role === 'TEACHER'
      ? teacherNav
      : studentNav;

  return (
    <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col shrink-0 min-h-[calc(100vh-4rem)]">
      <div className="p-4">
        <p className="px-3 text-[11px] font-bold uppercase tracking-wider text-slate-400 mb-2">
          {user.role} Navigation
        </p>
        <nav className="space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/management' || item.to === '/teacher' || item.to === '/student'}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-blue-600 text-white shadow-sm font-semibold'
                      : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                  }`
                }
              >
                <Icon className="w-4 h-4 shrink-0" />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>
      </div>

      <div className="mt-auto p-4 border-t border-slate-800/80 bg-slate-950/40">
        <div className="bg-slate-800/60 rounded-lg p-3 text-xs text-slate-400">
          <p className="font-semibold text-slate-200">ExamForge v1.0</p>
          <p className="mt-0.5">Secure AI College Examination System</p>
        </div>
      </div>
    </aside>
  );
};
