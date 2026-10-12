import React, { useState, useEffect } from 'react';
import { apiClient } from '../../api/client';
import { ExamSchedule, ClassAnalytics } from '../../types';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Download, BarChart3, Users, Award, TrendingUp, CheckCircle, XCircle } from 'lucide-react';

export const TeacherAnalyticsPage: React.FC = () => {
  const [schedules, setSchedules] = useState<ExamSchedule[]>([]);
  const [selectedScheduleId, setSelectedScheduleId] = useState<number | null>(null);
  const [analytics, setAnalytics] = useState<ClassAnalytics | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadSchedules();
  }, []);

  useEffect(() => {
    if (selectedScheduleId) {
      loadAnalytics(selectedScheduleId);
    }
  }, [selectedScheduleId]);

  const loadSchedules = async () => {
    try {
      const res = await apiClient.get<ExamSchedule[]>('/schedules');
      setSchedules(res.data);
      if (res.data.length > 0) {
        setSelectedScheduleId(res.data[0].id);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const loadAnalytics = async (schedId: number) => {
    setIsLoading(true);
    try {
      const res = await apiClient.get<ClassAnalytics>(`/results/class/${schedId}`);
      setAnalytics(res.data);
    } catch (err) {
      console.error(err);
      setAnalytics(null);
    } finally {
      setIsLoading(false);
    }
  };

  const handleExportCsv = async () => {
    if (!selectedScheduleId) return;
    try {
      const response = await apiClient.get(`/results/class/${selectedScheduleId}/csv`, {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `exam_results_${selectedScheduleId}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert('Failed to export CSV results');
    }
  };

  if (isLoading && schedules.length === 0) {
    return <LoadingSpinner message="Loading performance analytics..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <BarChart3 className="w-6 h-6 text-blue-600" />
            Class Examination Performance & Analytics
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Review submission grade rosters, student rankings, pass metrics, and export CSV reports.
          </p>
        </div>
        {analytics && (
          <button
            onClick={handleExportCsv}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-sm transition flex items-center gap-2"
          >
            <Download className="w-4 h-4" /> Export CSV Roster
          </button>
        )}
      </div>

      {/* Schedule Picker Bar */}
      <div className="flex items-center gap-4 bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
        <label className="text-xs font-bold uppercase text-slate-500">Select Exam Schedule:</label>
        <select
          value={selectedScheduleId || ''}
          onChange={(e) => setSelectedScheduleId(Number(e.target.value))}
          className="border border-slate-200 rounded-lg py-2 px-3 text-sm font-semibold text-slate-800 bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-600"
        >
          {schedules.map((s) => (
            <option key={s.id} value={s.id}>
              {s.title} — {s.className} ({new Date(s.startTime).toLocaleDateString()})
            </option>
          ))}
        </select>
      </div>

      {analytics ? (
        <div className="space-y-6">
          {/* Summary Metric Cards */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
              <span className="text-xs font-bold uppercase text-slate-400">Class Attendance</span>
              <p className="text-2xl font-extrabold text-slate-900 mt-1">
                {analytics.attemptsCount} / {analytics.totalEnrolled}
              </p>
              <span className="text-[11px] text-slate-500">Students attended</span>
            </div>

            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
              <span className="text-xs font-bold uppercase text-slate-400">Class Average</span>
              <p className="text-2xl font-extrabold text-blue-600 mt-1">
                {analytics.averageScore} <span className="text-xs text-slate-400">/ {analytics.maxMarks}</span>
              </p>
              <span className="text-[11px] text-slate-500">Mean raw score</span>
            </div>

            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
              <span className="text-xs font-bold uppercase text-slate-400">Pass Rate</span>
              <p className="text-2xl font-extrabold text-emerald-600 mt-1">
                {analytics.passPercentage}%
              </p>
              <span className="text-[11px] text-slate-500">{analytics.passedCount} students passed</span>
            </div>

            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
              <span className="text-xs font-bold uppercase text-slate-400">Score Range</span>
              <p className="text-2xl font-extrabold text-purple-600 mt-1">
                {analytics.highestScore} <span className="text-xs text-slate-400">to {analytics.lowestScore}</span>
              </p>
              <span className="text-[11px] text-slate-500">High / Low Marks</span>
            </div>
          </div>

          {/* Student Rankings Table */}
          <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
            <div className="p-4 border-b border-slate-100 flex items-center justify-between">
              <h3 className="text-sm font-bold uppercase tracking-wider text-slate-800">
                Student Examination Results & Rankings
              </h3>
              <span className="text-xs text-slate-500">{analytics.rankings.length} Submissions</span>
            </div>

            {analytics.rankings.length === 0 ? (
              <div className="p-8 text-center text-slate-500 text-sm">
                No student attempts recorded for this exam yet.
              </div>
            ) : (
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase">
                  <tr>
                    <th className="py-3 px-4">Rank</th>
                    <th className="py-3 px-4">Roll Number</th>
                    <th className="py-3 px-4">Student</th>
                    <th className="py-3 px-4">Marks Obtained</th>
                    <th className="py-3 px-4">Percentage</th>
                    <th className="py-3 px-4 text-right">Result</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {analytics.rankings.map((r) => (
                    <tr key={r.studentId} className="hover:bg-slate-50/50 transition">
                      <td className="py-3.5 px-4 font-extrabold text-slate-700">#{r.rank}</td>
                      <td className="py-3.5 px-4 font-mono font-bold text-blue-700">{r.rollNumber}</td>
                      <td className="py-3.5 px-4 font-semibold text-slate-900">{r.studentName}</td>
                      <td className="py-3.5 px-4 font-bold text-slate-800">
                        {r.score} / {analytics.maxMarks}
                      </td>
                      <td className="py-3.5 px-4 font-semibold text-slate-700">{r.percentage}%</td>
                      <td className="py-3.5 px-4 text-right">
                        <span
                          className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold border ${
                            r.passed
                              ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                              : 'bg-rose-50 text-rose-700 border-rose-200'
                          }`}
                        >
                          {r.passed ? <CheckCircle className="w-3 h-3" /> : <XCircle className="w-3 h-3" />}
                          {r.passed ? 'PASSED' : 'FAILED'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      ) : (
        <div className="bg-white p-12 text-center text-slate-500 rounded-xl border border-slate-200 text-sm">
          Select a scheduled exam above to inspect class analytics.
        </div>
      )}
    </div>
  );
};
