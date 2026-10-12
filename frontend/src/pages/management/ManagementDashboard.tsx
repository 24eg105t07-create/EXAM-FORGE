import React, { useState, useEffect } from 'react';
import { apiClient } from '../../api/client';
import { Users, GraduationCap, BookOpen, Calendar, CheckCircle2, TrendingUp, ShieldCheck } from 'lucide-react';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Link } from 'react-router-dom';

export const ManagementDashboard: React.FC = () => {
  const [stats, setStats] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const res = await apiClient.get('/management/users/stats/college');
      setStats(res.data);
    } catch (err) {
      console.error('Failed to load stats', err);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading college administration metrics..." />;
  }

  const statCards = [
    { title: 'Total Registered Users', value: stats?.totalUsers || 0, icon: Users, color: 'text-blue-600', bg: 'bg-blue-50' },
    { title: 'Faculty Members', value: stats?.totalTeachers || 0, icon: BookOpen, color: 'text-indigo-600', bg: 'bg-indigo-50' },
    { title: 'Active Students', value: stats?.totalStudents || 0, icon: GraduationCap, color: 'text-emerald-600', bg: 'bg-emerald-50' },
    { title: 'Academic Subjects', value: stats?.totalSubjects || 0, icon: BookOpen, color: 'text-amber-600', bg: 'bg-amber-50' },
    { title: 'Scheduled Exams', value: stats?.totalExamsScheduled || 0, icon: Calendar, color: 'text-purple-600', bg: 'bg-purple-50' },
    { title: 'Exam Submissions', value: stats?.totalExamAttempts || 0, icon: CheckCircle2, color: 'text-teal-600', bg: 'bg-teal-50' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">College Management Control Center</h1>
          <p className="text-sm text-slate-500 mt-1">
            Supervise academic departments, user provisioning, exam schedules, and audit records.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Link
            to="/management/schedules"
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-semibold shadow-sm transition"
          >
            + Schedule Examination
          </Link>
          <Link
            to="/management/users"
            className="px-4 py-2 bg-white hover:bg-slate-50 border border-slate-300 text-slate-700 rounded-lg text-sm font-semibold transition"
          >
            Manage Users
          </Link>
        </div>
      </div>

      {/* Metric Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {statCards.map((card, idx) => {
          const Icon = card.icon;
          return (
            <div key={idx} className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{card.title}</p>
                <p className="text-2xl font-extrabold text-slate-900 mt-1">{card.value}</p>
              </div>
              <div className={`p-3 rounded-xl ${card.bg}`}>
                <Icon className={`w-6 h-6 ${card.color}`} />
              </div>
            </div>
          );
        })}
      </div>

      {/* Operational Highlights */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-6">
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs">
          <h3 className="text-base font-bold text-slate-900 mb-3 flex items-center gap-2">
            <ShieldCheck className="w-5 h-5 text-purple-600" />
            Security & Compliance Rules
          </h3>
          <ul className="text-xs text-slate-600 space-y-2.5">
            <li className="flex items-start gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-blue-600 mt-1.5 shrink-0" />
              <span><strong>Restricted Provisioning:</strong> Public users cannot register as faculty or management. All staff accounts must be provisioned here.</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-blue-600 mt-1.5 shrink-0" />
              <span><strong>Approved Exam Snapshots:</strong> Only papers approved by designated faculty can be scheduled. Once scheduled, questions cannot be silently modified.</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-blue-600 mt-1.5 shrink-0" />
              <span><strong>Automated Conflict Checking:</strong> The engine automatically checks room and class overlap before approving new exam schedules.</span>
            </li>
          </ul>
        </div>

        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs">
          <h3 className="text-base font-bold text-slate-900 mb-3 flex items-center gap-2">
            <TrendingUp className="w-5 h-5 text-emerald-600" />
            Quick Navigation
          </h3>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <Link to="/management/users" className="p-3 rounded-lg border border-slate-100 bg-slate-50 hover:bg-slate-100 font-semibold text-slate-800 transition">
              User Directory & Roles →
            </Link>
            <Link to="/management/academic" className="p-3 rounded-lg border border-slate-100 bg-slate-50 hover:bg-slate-100 font-semibold text-slate-800 transition">
              Academic Structure & Batches →
            </Link>
            <Link to="/management/schedules" className="p-3 rounded-lg border border-slate-100 bg-slate-50 hover:bg-slate-100 font-semibold text-slate-800 transition">
              Live & Upcoming Schedules →
            </Link>
            <Link to="/management/audit-logs" className="p-3 rounded-lg border border-slate-100 bg-slate-50 hover:bg-slate-100 font-semibold text-slate-800 transition">
              Audit Logs & Security Trail →
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
