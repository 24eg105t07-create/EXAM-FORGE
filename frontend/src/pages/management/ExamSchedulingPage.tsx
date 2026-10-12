import React, { useState, useEffect } from 'react';
import { apiClient } from '../../api/client';
import { ExamSchedule, ExamPaper, AcademicClass, Section, ResultReleasePolicy } from '../../types';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { StatusBadge } from '../../components/common/StatusBadge';
import { Modal } from '../../components/common/Modal';
import { Calendar, Plus, Clock, Users, XCircle, CheckCircle } from 'lucide-react';

export const ExamSchedulingPage: React.FC = () => {
  const [schedules, setSchedules] = useState<ExamSchedule[]>([]);
  const [approvedPapers, setApprovedPapers] = useState<ExamPaper[]>([]);
  const [classes, setClasses] = useState<AcademicClass[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Modal State
  const [isScheduleModalOpen, setIsScheduleModalOpen] = useState(false);
  const [cancelScheduleId, setCancelScheduleId] = useState<number | null>(null);
  const [cancelReason, setCancelReason] = useState('');

  // Form State
  const [paperId, setPaperId] = useState<number | ''>('');
  const [classId, setClassId] = useState<number | ''>('');
  const [title, setTitle] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [durationMinutes, setDurationMinutes] = useState<number>(60);
  const [maxMarks, setMaxMarks] = useState<number>(20);
  const [instructions, setInstructions] = useState('');
  const [resultPolicy, setResultPolicy] = useState<ResultReleasePolicy>('IMMEDIATE');

  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setIsLoading(true);
    try {
      const [schedRes, paperRes, classRes] = await Promise.all([
        apiClient.get<ExamSchedule[]>('/schedules'),
        apiClient.get<ExamPaper[]>('/teacher/papers/approved'),
        apiClient.get<AcademicClass[]>('/academic/classes'),
      ]);
      setSchedules(schedRes.data);
      setApprovedPapers(paperRes.data);
      setClasses(classRes.data);
    } catch (err) {
      console.error('Failed to load scheduling data', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handlePaperSelect = (id: number) => {
    setPaperId(id);
    const paper = approvedPapers.find((p) => p.id === id);
    if (paper) {
      setTitle(paper.title);
      setDurationMinutes(paper.durationMinutes);
      setMaxMarks(paper.maxMarks);
    }
  };

  const handleCreateSchedule = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setIsSubmitting(true);

    try {
      await apiClient.post('/management/schedules', {
        examPaperId: Number(paperId),
        classId: Number(classId),
        sectionId: null,
        title,
        startTime: startTime + ':00',
        endTime: endTime + ':00',
        durationMinutes: Number(durationMinutes),
        maxMarks: Number(maxMarks),
        instructions,
        resultReleasePolicy: resultPolicy,
      });
      setIsScheduleModalOpen(false);
      loadData();
    } catch (err: any) {
      setFormError(err.response?.data?.message || 'Failed to schedule exam. Check time conflicts or student enrollments.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancelConfirm = async () => {
    if (!cancelScheduleId || !cancelReason.trim()) return;
    try {
      await apiClient.post(`/management/schedules/${cancelScheduleId}/cancel`, {
        reason: cancelReason,
      });
      setCancelScheduleId(null);
      setCancelReason('');
      loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel schedule');
    }
  };

  const handleReleaseResults = async (id: number) => {
    try {
      await apiClient.post(`/management/schedules/${id}/release-results`);
      alert('Examination results have been released!');
      loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to release results');
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading examination schedule roster..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Examination Scheduling Center</h1>
          <p className="text-sm text-slate-500 mt-1">
            Publish approved question papers to classes, manage exam windows, and release results.
          </p>
        </div>
        <button
          onClick={() => {
            // Set default default times for convenient demo
            const now = new Date();
            const tomorrow = new Date(now.getTime() + 10 * 60000); // 10 mins from now
            const end = new Date(tomorrow.getTime() + 24 * 3600 * 1000); // 24 hours window
            setStartTime(tomorrow.toISOString().slice(0, 16));
            setEndTime(end.toISOString().slice(0, 16));
            setIsScheduleModalOpen(true);
          }}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-semibold shadow-sm transition flex items-center gap-2"
        >
          <Plus className="w-4 h-4" /> Schedule Approved Exam
        </button>
      </div>

      {/* Schedules Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
        {schedules.length === 0 ? (
          <div className="text-center py-12 text-slate-500 text-sm">
            No examinations scheduled yet. Once teachers approve question papers, schedule them here.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50/80 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase">
                <tr>
                  <th className="py-3 px-4">Exam Details</th>
                  <th className="py-3 px-4">Class & Roster</th>
                  <th className="py-3 px-4">Window & Duration</th>
                  <th className="py-3 px-4">Policy & Results</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {schedules.map((s) => (
                  <tr key={s.id} className="hover:bg-slate-50/50 transition">
                    <td className="py-3.5 px-4">
                      <div className="font-bold text-slate-900">{s.title}</div>
                      <div className="text-xs text-blue-700 font-semibold mt-0.5">{s.subjectName}</div>
                      <div className="text-xs text-slate-500">Max Marks: {s.maxMarks}</div>
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="font-medium text-slate-800">{s.className}</div>
                      <div className="text-xs text-slate-500 flex items-center gap-1 mt-0.5">
                        <Users className="w-3.5 h-3.5" />
                        <span>{s.eligibleStudentsCount} students enrolled</span>
                      </div>
                    </td>
                    <td className="py-3.5 px-4 text-xs text-slate-600">
                      <div className="font-semibold text-slate-800">
                        {new Date(s.startTime).toLocaleDateString()}
                      </div>
                      <div>
                        {new Date(s.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} —{' '}
                        {new Date(s.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </div>
                      <div className="text-blue-700 font-medium flex items-center gap-1 mt-0.5">
                        <Clock className="w-3 h-3" /> {s.durationMinutes} minutes
                      </div>
                    </td>
                    <td className="py-3.5 px-4 text-xs">
                      <span className="font-semibold text-slate-700 block">{s.resultReleasePolicy}</span>
                      {s.isResultsReleased ? (
                        <span className="text-emerald-700 font-bold flex items-center gap-1 mt-0.5">
                          <CheckCircle className="w-3 h-3" /> Released
                        </span>
                      ) : (
                        <span className="text-amber-700 font-medium">Unreleased</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4">
                      <StatusBadge status={s.status} />
                    </td>
                    <td className="py-3.5 px-4 text-right space-x-2">
                      {!s.isResultsReleased && s.status !== 'CANCELLED' && (
                        <button
                          onClick={() => handleReleaseResults(s.id)}
                          className="px-2.5 py-1 text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200 rounded-lg hover:bg-emerald-100 transition"
                          title="Release results now"
                        >
                          Release Results
                        </button>
                      )}
                      {s.status === 'SCHEDULED' && (
                        <button
                          onClick={() => setCancelScheduleId(s.id)}
                          className="px-2.5 py-1 text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200 rounded-lg hover:bg-rose-100 transition"
                        >
                          Cancel
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Schedule Exam Modal */}
      <Modal isOpen={isScheduleModalOpen} onClose={() => setIsScheduleModalOpen(false)} title="Schedule Examination">
        {formError && (
          <div className="mb-4 p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs">
            {formError}
          </div>
        )}
        <form onSubmit={handleCreateSchedule} className="space-y-4">
          <div>
            <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Approved Question Paper</label>
            <select
              required
              value={paperId}
              onChange={(e) => handlePaperSelect(Number(e.target.value))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white"
            >
              <option value="">Select an Approved Paper</option>
              {approvedPapers.map((p) => (
                <option key={p.id} value={p.id}>
                  [{p.subjectCode}] {p.title} ({p.totalQuestions} Questions, Max Marks: {p.maxMarks})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Target Class</label>
            <select
              required
              value={classId}
              onChange={(e) => setClassId(Number(e.target.value))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white"
            >
              <option value="">Select Class Batch</option>
              {classes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.code})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Exam Title</label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Start Time (Window Open)</label>
              <input
                type="datetime-local"
                required
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-bold uppercase text-slate-700 mb-1">End Time (Window Deadline)</label>
              <input
                type="datetime-local"
                required
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
              />
            </div>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Duration (Min)</label>
              <input
                type="number"
                required
                min={5}
                value={durationMinutes}
                onChange={(e) => setDurationMinutes(Number(e.target.value))}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Maximum Marks</label>
              <input
                type="number"
                required
                value={maxMarks}
                onChange={(e) => setMaxMarks(Number(e.target.value))}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Result Release</label>
              <select
                value={resultPolicy}
                onChange={(e) => setResultPolicy(e.target.value as ResultReleasePolicy)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white"
              >
                <option value="IMMEDIATE">Immediate</option>
                <option value="AFTER_EXAM_WINDOW">After Window Closes</option>
                <option value="MANUAL">Manual Control</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Instructions for Students</label>
            <textarea
              value={instructions}
              onChange={(e) => setInstructions(e.target.value)}
              rows={2}
              placeholder="All questions are compulsory. Ensure steady internet connectivity."
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
            />
          </div>

          <div className="pt-4 flex justify-end gap-3 border-t border-slate-100">
            <button
              type="button"
              onClick={() => setIsScheduleModalOpen(false)}
              className="px-4 py-2 border border-slate-300 rounded-lg text-sm font-semibold text-slate-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-semibold disabled:opacity-50"
            >
              {isSubmitting ? 'Scheduling...' : 'Confirm Schedule'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Cancel Reason Modal */}
      <Modal isOpen={!!cancelScheduleId} onClose={() => setCancelScheduleId(null)} title="Cancel Scheduled Examination">
        <p className="text-sm text-slate-600 mb-4">
          Please provide a formal justification for cancelling this examination. An immutable audit record will be registered.
        </p>
        <textarea
          required
          value={cancelReason}
          onChange={(e) => setCancelReason(e.target.value)}
          placeholder="e.g. Rescheduling due to academic calendar clash"
          rows={3}
          className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm mb-4"
        />
        <div className="flex justify-end gap-2">
          <button
            onClick={() => setCancelScheduleId(null)}
            className="px-4 py-2 border border-slate-300 rounded-lg text-sm font-semibold text-slate-700"
          >
            Close
          </button>
          <button
            onClick={handleCancelConfirm}
            className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-sm font-semibold"
          >
            Confirm Cancellation
          </button>
        </div>
      </Modal>
    </div>
  );
};
