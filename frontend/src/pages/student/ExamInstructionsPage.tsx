import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { apiClient } from '../../api/client';
import { ExamSchedule } from '../../types';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import {
  FileText,
  Clock,
  Award,
  AlertTriangle,
  CheckCircle2,
  ArrowRight,
  ShieldAlert,
  HelpCircle,
  ArrowLeft
} from 'lucide-react';

export const ExamInstructionsPage: React.FC = () => {
  const { scheduleId } = useParams<{ scheduleId: string }>();
  const navigate = useNavigate();
  const [schedule, setSchedule] = useState<ExamSchedule | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [acknowledged, setAcknowledged] = useState(false);
  const [isStarting, setIsStarting] = useState(false);

  useEffect(() => {
    loadScheduleDetails();
  }, [scheduleId]);

  const loadScheduleDetails = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await apiClient.get<ExamSchedule>(`/schedules/${scheduleId}`);
      setSchedule(res.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load examination details.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleStartExam = async () => {
    if (!acknowledged || !scheduleId) return;
    setIsStarting(true);
    try {
      // Pre-flight check / trigger start
      await apiClient.post(`/student/exams/${scheduleId}/start`);
      navigate(`/student/exams/${scheduleId}/room`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to initialize examination session.');
      setIsStarting(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading exam rules and schedule details..." />;
  }

  if (error || !schedule) {
    return (
      <div className="max-w-2xl mx-auto p-6 bg-white rounded-xl border border-red-200 text-center">
        <AlertTriangle className="w-12 h-12 text-red-500 mx-auto mb-3" />
        <h3 className="text-lg font-bold text-slate-900">Examination Unavailable</h3>
        <p className="text-sm text-red-600 mt-1">{error || 'Schedule not found'}</p>
        <Link
          to="/student"
          className="mt-4 inline-flex items-center gap-1.5 px-4 py-2 bg-slate-800 text-white text-xs font-semibold rounded-lg hover:bg-slate-900"
        >
          <ArrowLeft className="w-4 h-4" /> Back to Student Dashboard
        </Link>
      </div>
    );
  }

  const startTime = new Date(schedule.startTime);
  const endTime = new Date(schedule.endTime);

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Top Banner */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-xs font-bold text-blue-700 bg-blue-50 px-2.5 py-1 rounded border border-blue-200">
            {schedule.subjectName}
          </span>
          <h1 className="text-2xl font-black text-slate-900 mt-2">{schedule.title}</h1>
          <p className="text-xs text-slate-500 mt-1">
            Class: <span className="font-semibold text-slate-700">{schedule.className}</span> {schedule.sectionName ? `(${schedule.sectionName})` : ''}
          </p>
        </div>
        <div className="flex gap-4 border-l border-slate-200 pl-4">
          <div className="text-center">
            <span className="text-xs text-slate-400 font-bold block">DURATION</span>
            <span className="text-lg font-black text-slate-900 flex items-center justify-center gap-1">
              <Clock className="w-4 h-4 text-blue-600" />
              {schedule.durationMinutes}m
            </span>
          </div>
          <div className="text-center">
            <span className="text-xs text-slate-400 font-bold block">MAX MARKS</span>
            <span className="text-lg font-black text-emerald-600 flex items-center justify-center gap-1">
              <Award className="w-4 h-4 text-emerald-500" />
              {schedule.maxMarks}
            </span>
          </div>
        </div>
      </div>

      {/* Schedule Window Information */}
      <div className="p-4 rounded-xl bg-blue-50/70 border border-blue-200 flex items-center justify-between text-xs text-blue-900">
        <div>
          <span className="font-bold">Scheduled Testing Window: </span>
          <span>
            {startTime.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })} —{' '}
            {endTime.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })}
          </span>
        </div>
        <span className="font-mono bg-blue-200/60 px-2 py-0.5 rounded text-blue-800 font-semibold">
          Server Synced Timer
        </span>
      </div>

      {/* Rules & Instructions */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs space-y-6">
        <div>
          <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
            <FileText className="w-5 h-5 text-indigo-600" />
            Official Examination Instructions & Code of Conduct
          </h2>
          <p className="text-xs text-slate-500 mt-1">
            Please read the instructions carefully before launching the secure test environment.
          </p>
        </div>

        {schedule.instructions && (
          <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 text-xs text-slate-700 leading-relaxed">
            <span className="font-bold block text-slate-900 mb-1">Teacher Instructions:</span>
            {schedule.instructions}
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
          <div className="p-4 rounded-xl border border-slate-100 bg-slate-50/60 space-y-2">
            <div className="font-bold text-slate-900 flex items-center gap-1.5">
              <Clock className="w-4 h-4 text-blue-600" /> Server-Enforced Time Management
            </div>
            <ul className="list-disc list-inside text-slate-600 space-y-1">
              <li>Once you click Start, the examination countdown will begin immediately.</li>
              <li>The timer cannot be paused. Refreshing the browser will resume your session with server elapsed time.</li>
              <li>The examination will auto-submit when remaining time reaches 00:00.</li>
            </ul>
          </div>

          <div className="p-4 rounded-xl border border-slate-100 bg-slate-50/60 space-y-2">
            <div className="font-bold text-slate-900 flex items-center gap-1.5">
              <ShieldAlert className="w-4 h-4 text-amber-600" /> Integrity & Autosafe
            </div>
            <ul className="list-disc list-inside text-slate-600 space-y-1">
              <li>Every answer is securely auto-saved to the server as soon as you select an option.</li>
              <li>You can flag any question for review and revisit it using the Question Palette.</li>
              <li>Switching tabs, closing the window, or tampering with the console is recorded in the audit log.</li>
            </ul>
          </div>
        </div>

        {/* Legend */}
        <div className="p-4 rounded-xl border border-slate-200 bg-white">
          <span className="text-xs font-bold text-slate-800 block mb-2">Question Palette Color Codes:</span>
          <div className="flex flex-wrap gap-4 text-xs text-slate-600">
            <span className="flex items-center gap-1.5">
              <span className="w-4 h-4 rounded bg-emerald-600 text-white flex items-center justify-center text-[10px] font-bold">1</span>
              Answered
            </span>
            <span className="flex items-center gap-1.5">
              <span className="w-4 h-4 rounded bg-amber-500 text-white flex items-center justify-center text-[10px] font-bold">2</span>
              Marked for Review
            </span>
            <span className="flex items-center gap-1.5">
              <span className="w-4 h-4 rounded bg-slate-200 text-slate-700 flex items-center justify-center text-[10px] font-bold">3</span>
              Unanswered / Not Visited
            </span>
          </div>
        </div>

        {/* Honor code acknowledgment checkbox */}
        <div className="pt-4 border-t border-slate-200">
          <label className="flex items-start gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={acknowledged}
              onChange={(e) => setAcknowledged(e.target.checked)}
              className="mt-1 h-4 w-4 text-blue-600 rounded border-slate-300 focus:ring-blue-500"
            />
            <span className="text-xs text-slate-700 leading-normal">
              I certify that I am the authorized student enrolled in this course. I will complete this examination independently without unauthorized aids, browser extensions, or assistance. I understand that my answers will be automatically graded and audited.
            </span>
          </label>
        </div>

        {/* Action button */}
        <div className="flex items-center justify-between pt-2">
          <Link
            to="/student"
            className="px-4 py-2 border border-slate-300 text-slate-700 rounded-lg text-xs font-bold hover:bg-slate-50 transition"
          >
            Cancel & Return
          </Link>
          <button
            onClick={handleStartExam}
            disabled={!acknowledged || isStarting}
            className={`px-6 py-2.5 rounded-lg text-xs font-bold text-white transition flex items-center gap-2 shadow-sm ${
              acknowledged && !isStarting
                ? 'bg-blue-600 hover:bg-blue-700 cursor-pointer'
                : 'bg-slate-300 cursor-not-allowed'
            }`}
          >
            {isStarting ? (
              <>
                <LoadingSpinner size="sm" />
                <span>Initializing Exam Session...</span>
              </>
            ) : (
              <>
                <span>I Understand the Rules — Begin Exam</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
