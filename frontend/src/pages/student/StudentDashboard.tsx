import React, { useState, useEffect } from 'react';
import { apiClient } from '../../api/client';
import { ExamSchedule, StudentAttemptSummary } from '../../types';
import { useAuth } from '../../context/AuthContext';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { StatusBadge } from '../../components/common/StatusBadge';
import { Link } from 'react-router-dom';
import { GraduationCap, Calendar, Clock, Award, ArrowRight, CheckCircle2, History } from 'lucide-react';

export const StudentDashboard: React.FC = () => {
  const { user } = useAuth();
  const [availableExams, setAvailableExams] = useState<ExamSchedule[]>([]);
  const [attemptHistory, setAttemptHistory] = useState<StudentAttemptSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadStudentDashboard();
  }, []);

  const loadStudentDashboard = async () => {
    setIsLoading(true);
    try {
      const [examRes, histRes] = await Promise.all([
        apiClient.get<ExamSchedule[]>('/student/available-schedules'),
        apiClient.get<StudentAttemptSummary[]>('/results/my-history'),
      ]);
      setAvailableExams(examRes.data);
      setAttemptHistory(histRes.data);
    } catch (err) {
      console.error('Failed to load student dashboard', err);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading your examination student portal..." />;
  }

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-blue-700 to-indigo-700 text-white shadow-md">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-white/20 rounded-xl">
              <GraduationCap className="w-8 h-8 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight">Welcome back, {user?.fullName}!</h1>
              <p className="text-xs text-blue-100 mt-0.5">
                Institutional Student Examination Portal • {user?.email}
              </p>
            </div>
          </div>
          <div className="text-right">
            <span className="text-xs font-mono font-bold px-3 py-1 bg-white/20 rounded-lg">
              Status: Verified Active
            </span>
          </div>
        </div>
      </div>

      {/* Available / Upcoming Examinations */}
      <div className="space-y-3">
        <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
          <Calendar className="w-5 h-5 text-blue-600" />
          Available & Upcoming Examinations
        </h3>

        {availableExams.length === 0 ? (
          <div className="bg-white p-8 rounded-xl border border-slate-200 text-center text-slate-500 text-sm">
            No examinations scheduled for your class at this time. Check back later or review your notifications.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {availableExams.map((exam) => {
              const now = new Date();
              const start = new Date(exam.startTime);
              const end = new Date(exam.endTime);
              const isOpen = now >= start && now <= end;
              const hasAttempted = attemptHistory.some((a) => a.scheduleId === exam.id);

              return (
                <div
                  key={exam.id}
                  className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs flex flex-col justify-between hover:border-blue-300 transition"
                >
                  <div>
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-blue-700 bg-blue-50 px-2.5 py-0.5 rounded border border-blue-200">
                        {exam.subjectName}
                      </span>
                      <StatusBadge status={hasAttempted ? 'COMPLETED' : isOpen ? 'IN_PROGRESS' : 'SCHEDULED'} />
                    </div>

                    <h4 className="font-bold text-slate-900 text-base mt-2">{exam.title}</h4>

                    <div className="mt-3 space-y-1.5 text-xs text-slate-500">
                      <div className="flex items-center gap-2">
                        <Clock className="w-4 h-4 text-slate-400" />
                        <span>Duration: {exam.durationMinutes} Minutes • Max Marks: {exam.maxMarks}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Calendar className="w-4 h-4 text-slate-400" />
                        <span>
                          Window: {start.toLocaleDateString()} {start.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} —{' '}
                          {end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between">
                    {hasAttempted ? (
                      <span className="text-xs text-emerald-700 font-bold flex items-center gap-1">
                        <CheckCircle2 className="w-4 h-4" /> Exam Submitted
                      </span>
                    ) : (
                      <Link
                        to={`/student/exams/${exam.id}/instructions`}
                        className="w-full py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-bold transition flex items-center justify-center gap-1.5 shadow-sm"
                      >
                        <span>View Instructions & Start</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </Link>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Recent Attempts & Scores */}
      <div className="mt-8 space-y-3">
        <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
          <History className="w-5 h-5 text-indigo-600" />
          My Examination History & Results
        </h3>

        <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
          {attemptHistory.length === 0 ? (
            <div className="p-8 text-center text-slate-500 text-sm">
              You haven't attempted any examinations yet.
            </div>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase">
                <tr>
                  <th className="py-3 px-4">Examination</th>
                  <th className="py-3 px-4">Subject</th>
                  <th className="py-3 px-4">Attempt Status</th>
                  <th className="py-3 px-4">Marks Obtained</th>
                  <th className="py-3 px-4 text-right">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {attemptHistory.map((att) => (
                  <tr key={att.attemptId} className="hover:bg-slate-50/50 transition">
                    <td className="py-3.5 px-4 font-semibold text-slate-900">{att.examTitle}</td>
                    <td className="py-3.5 px-4 text-xs font-medium text-slate-700">{att.subjectName}</td>
                    <td className="py-3.5 px-4">
                      <StatusBadge status={att.status} />
                    </td>
                    <td className="py-3.5 px-4">
                      {att.isResultReleased && att.totalScore !== undefined ? (
                        <div className="text-xs">
                          <span className="font-bold text-slate-900 text-sm">{att.totalScore}</span>
                          <span className="text-slate-400"> / {att.maxMarks}</span>
                          <span className={`ml-2 font-bold ${att.passed ? 'text-emerald-700' : 'text-rose-700'}`}>
                            ({att.percentage}%)
                          </span>
                        </div>
                      ) : (
                        <span className="text-xs text-amber-600 font-medium italic">
                          Results Pending Release
                        </span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      {att.isResultReleased ? (
                        <Link
                          to={`/student/results/${att.attemptId}`}
                          className="px-3 py-1 bg-slate-100 hover:bg-blue-50 text-blue-700 rounded-lg text-xs font-semibold transition"
                        >
                          View Scorecard →
                        </Link>
                      ) : (
                        <span className="text-xs text-slate-400">Unreleased</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
};
