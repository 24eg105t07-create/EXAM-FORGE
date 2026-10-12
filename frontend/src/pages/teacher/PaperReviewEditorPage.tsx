import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiClient } from '../../api/client';
import { ExamPaper, QuestionItem, OptionItem, PaperQuestion, Difficulty } from '../../types';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { StatusBadge } from '../../components/common/StatusBadge';
import { Modal } from '../../components/common/Modal';
import {
  ClipboardCheck,
  Edit3,
  Trash2,
  RefreshCw,
  CheckCircle,
  Lock,
  ArrowLeft,
  Sparkles,
  BookOpen,
  Award,
} from 'lucide-react';

export const PaperReviewEditorPage: React.FC = () => {
  const { paperId } = useParams<{ paperId: string }>();
  const navigate = useNavigate();

  const [paper, setPaper] = useState<ExamPaper | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Edit Question Modal State
  const [editingQuestion, setEditingQuestion] = useState<QuestionItem | null>(null);
  const [editText, setEditText] = useState('');
  const [editDiff, setEditDiff] = useState<Difficulty>('MEDIUM');
  const [editMarks, setEditMarks] = useState<number>(1);
  const [editExplanation, setEditExplanation] = useState('');
  const [editOptions, setEditOptions] = useState<OptionItem[]>([]);

  // Regenerate Modal State
  const [regeneratingQuestionId, setRegeneratingQuestionId] = useState<number | null>(null);
  const [regenInstructions, setRegenInstructions] = useState('');
  const [isRegenerating, setIsRegenerating] = useState(false);

  const [isApproving, setIsApproving] = useState(false);

  useEffect(() => {
    if (paperId) {
      loadPaper(Number(paperId));
    }
  }, [paperId]);

  const loadPaper = async (id: number) => {
    setIsLoading(true);
    try {
      const res = await apiClient.get<ExamPaper>(`/teacher/papers/${id}`);
      setPaper(res.data);
    } catch (err) {
      console.error('Failed to load exam paper', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleOpenEdit = (q: QuestionItem) => {
    setEditingQuestion(q);
    setEditText(q.questionText);
    setEditDiff(q.difficulty);
    setEditMarks(q.marks);
    setEditExplanation(q.explanation || '');
    setEditOptions(JSON.parse(JSON.stringify(q.options || [])));
  };

  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!paper || !editingQuestion) return;

    try {
      const res = await apiClient.put<ExamPaper>(
        `/teacher/papers/${paper.id}/questions/${editingQuestion.id}`,
        {
          subjectId: editingQuestion.subjectId,
          lessonId: editingQuestion.lessonId,
          questionText: editText,
          questionType: editingQuestion.questionType,
          difficulty: editDiff,
          marks: editMarks,
          explanation: editExplanation,
          sourceReference: editingQuestion.sourceReference,
          options: editOptions,
        }
      );
      setPaper(res.data);
      setEditingQuestion(null);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update question');
    }
  };

  const handleDelete = async (questionId: number) => {
    if (!paper || !confirm('Are you sure you want to remove this question from the paper?')) return;
    try {
      const res = await apiClient.delete<ExamPaper>(`/teacher/papers/${paper.id}/questions/${questionId}`);
      setPaper(res.data);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to delete question');
    }
  };

  const handleRegenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!paper || !regeneratingQuestionId) return;

    setIsRegenerating(true);
    try {
      const res = await apiClient.post<ExamPaper>(
        `/teacher/papers/${paper.id}/questions/${regeneratingQuestionId}/regenerate`,
        { instructions: regenInstructions }
      );
      setPaper(res.data);
      setRegeneratingQuestionId(null);
      setRegenInstructions('');
    } catch (err: any) {
      alert(err.response?.data?.message || 'Regeneration failed');
    } finally {
      setIsRegenerating(false);
    }
  };

  const handleApprove = async () => {
    if (!paper) return;
    if (!confirm('Are you ready to approve this question paper? Once approved, it will be locked and available to management for scheduling.')) return;

    setIsApproving(true);
    try {
      const res = await apiClient.post<ExamPaper>(`/teacher/papers/${paper.id}/approve`);
      setPaper(res.data);
      alert('Examination paper approved and snapshot locked successfully!');
    } catch (err: any) {
      alert(err.response?.data?.message || 'Approval failed');
    } finally {
      setIsApproving(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading question paper review canvas..." />;
  }

  if (!paper) {
    return <div className="text-center py-12 text-slate-500">Exam paper not found.</div>;
  }

  const isLocked = paper.isLocked || paper.status === 'APPROVED' || paper.status === 'PUBLISHED';

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <button
            onClick={() => navigate('/teacher')}
            className="text-xs font-semibold text-slate-500 hover:text-slate-800 flex items-center gap-1 mb-2"
          >
            <ArrowLeft className="w-3.5 h-3.5" /> Back to Dashboard
          </button>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">{paper.title}</h1>
            <StatusBadge status={paper.status} />
          </div>
          <p className="text-xs text-slate-500 mt-1">
            [{paper.subjectCode}] {paper.subjectName} • {paper.totalQuestions} Questions • Max {paper.maxMarks} Marks • Version {paper.version}
          </p>
        </div>

        <div className="flex items-center gap-3">
          {!isLocked ? (
            <button
              onClick={handleApprove}
              disabled={isApproving}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-bold shadow-sm transition flex items-center gap-2 disabled:opacity-50"
            >
              <CheckCircle className="w-4 h-4" />
              <span>{isApproving ? 'Approving...' : 'Approve Final Paper'}</span>
            </button>
          ) : (
            <div className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-100 text-slate-700 rounded-lg text-xs font-bold border border-slate-200">
              <Lock className="w-4 h-4 text-slate-500" />
              <span>Locked Approved Snapshot (Version {paper.version})</span>
            </div>
          )}
        </div>
      </div>

      {/* Questions List */}
      <div className="space-y-4">
        {paper.questions.map((pq, idx) => {
          const q = pq.question;
          return (
            <div
              key={q.id}
              className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition space-y-3"
            >
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 pb-2 border-b border-slate-100">
                <div className="flex items-center gap-2">
                  <span className="w-7 h-7 rounded-lg bg-blue-50 text-blue-700 font-extrabold flex items-center justify-center text-xs">
                    Q{pq.questionOrder}
                  </span>
                  <span className="text-xs font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                    {q.questionType}
                  </span>
                  <span
                    className={`text-xs font-semibold px-2 py-0.5 rounded ${
                      q.difficulty === 'EASY'
                        ? 'bg-emerald-50 text-emerald-700'
                        : q.difficulty === 'MEDIUM'
                        ? 'bg-blue-50 text-blue-700'
                        : 'bg-purple-50 text-purple-700'
                    }`}
                  >
                    {q.difficulty}
                  </span>
                  <span className="text-xs text-slate-400">
                    • {q.marks} Mark{q.marks > 1 ? 's' : ''}
                  </span>
                </div>

                {!isLocked && (
                  <div className="flex items-center gap-1.5 text-xs">
                    <button
                      onClick={() => handleOpenEdit(q)}
                      className="px-2.5 py-1 text-slate-600 hover:text-blue-600 hover:bg-slate-50 rounded-md transition flex items-center gap-1"
                    >
                      <Edit3 className="w-3.5 h-3.5" /> Edit
                    </button>
                    <button
                      onClick={() => {
                        setRegeneratingQuestionId(q.id);
                        setRegenInstructions('');
                      }}
                      className="px-2.5 py-1 text-slate-600 hover:text-purple-600 hover:bg-slate-50 rounded-md transition flex items-center gap-1"
                    >
                      <RefreshCw className="w-3.5 h-3.5" /> Regenerate
                    </button>
                    <button
                      onClick={() => handleDelete(q.id)}
                      className="p-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-md transition"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )}
              </div>

              {/* Question Text */}
              <p className="text-sm font-semibold text-slate-900 leading-relaxed">
                {q.questionText}
              </p>

              {/* Options */}
              {q.options && q.options.length > 0 && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
                  {q.options.map((opt) => (
                    <div
                      key={opt.id || opt.optionLabel}
                      className={`p-2.5 rounded-lg border text-xs flex items-center gap-2.5 transition ${
                        opt.isCorrect
                          ? 'bg-emerald-50/70 border-emerald-300 text-emerald-950 font-medium'
                          : 'bg-slate-50 border-slate-200 text-slate-700'
                      }`}
                    >
                      <span
                        className={`w-5 h-5 rounded-full flex items-center justify-center font-bold text-[11px] shrink-0 ${
                          opt.isCorrect
                            ? 'bg-emerald-600 text-white'
                            : 'bg-slate-200 text-slate-700'
                        }`}
                      >
                        {opt.optionLabel}
                      </span>
                      <span>{opt.optionText}</span>
                      {opt.isCorrect && (
                        <span className="ml-auto text-[10px] font-bold text-emerald-700 uppercase">
                          Correct
                        </span>
                      )}
                    </div>
                  ))}
                </div>
              )}

              {/* Explanation & Source Grounding */}
              <div className="pt-2 text-xs text-slate-500 space-y-1">
                {q.explanation && (
                  <p>
                    <strong className="text-slate-700">Explanation:</strong> {q.explanation}
                  </p>
                )}
                {q.sourceReference && (
                  <p className="text-blue-700">
                    <strong>Source Grounding:</strong> {q.sourceReference}
                  </p>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Edit Question Modal */}
      <Modal isOpen={!!editingQuestion} onClose={() => setEditingQuestion(null)} title="Edit Question Details">
        <form onSubmit={handleSaveEdit} className="space-y-4">
          <div>
            <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Question Prompt</label>
            <textarea
              required
              rows={3}
              value={editText}
              onChange={(e) => setEditText(e.target.value)}
              className="w-full px-3 py-2 border rounded-lg text-sm"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Difficulty</label>
              <select
                value={editDiff}
                onChange={(e) => setEditDiff(e.target.value as Difficulty)}
                className="w-full px-3 py-2 border rounded-lg text-sm bg-white"
              >
                <option value="EASY">Easy</option>
                <option value="MEDIUM">Medium</option>
                <option value="HARD">Hard</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Marks</label>
              <input
                type="number"
                min={0.5}
                step={0.5}
                value={editMarks}
                onChange={(e) => setEditMarks(Number(e.target.value))}
                className="w-full px-3 py-2 border rounded-lg text-sm"
              />
            </div>
          </div>

          {/* Options editing for MCQ */}
          {editOptions.length > 0 && (
            <div className="space-y-2">
              <label className="block text-xs font-bold uppercase text-slate-700">Answer Options & Key</label>
              {editOptions.map((opt, idx) => (
                <div key={idx} className="flex items-center gap-2">
                  <span className="w-6 font-bold text-center text-xs text-slate-500">{opt.optionLabel}</span>
                  <input
                    type="text"
                    value={opt.optionText}
                    onChange={(e) => {
                      const updated = [...editOptions];
                      updated[idx].optionText = e.target.value;
                      setEditOptions(updated);
                    }}
                    className="flex-1 px-3 py-1.5 border rounded-lg text-xs"
                  />
                  <label className="flex items-center gap-1 text-xs text-slate-700 cursor-pointer">
                    <input
                      type="radio"
                      name="correctOption"
                      checked={opt.isCorrect}
                      onChange={() => {
                        const updated = editOptions.map((o, i) => ({ ...o, isCorrect: i === idx }));
                        setEditOptions(updated);
                      }}
                    />
                    <span>Correct</span>
                  </label>
                </div>
              ))}
            </div>
          )}

          <div>
            <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Explanation</label>
            <textarea
              rows={2}
              value={editExplanation}
              onChange={(e) => setEditExplanation(e.target.value)}
              className="w-full px-3 py-2 border rounded-lg text-sm"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t">
            <button
              type="button"
              onClick={() => setEditingQuestion(null)}
              className="px-4 py-2 border rounded-lg text-xs font-semibold text-slate-700"
            >
              Cancel
            </button>
            <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-lg text-xs font-semibold">
              Save Changes
            </button>
          </div>
        </form>
      </Modal>

      {/* Regenerate Question Modal */}
      <Modal isOpen={!!regeneratingQuestionId} onClose={() => setRegeneratingQuestionId(null)} title="AI Question Regeneration">
        <form onSubmit={handleRegenerate} className="space-y-4">
          <p className="text-xs text-slate-600">
            Regenerate this specific question while preserving difficulty and curriculum context. You may enter optional instructions to guide the replacement question.
          </p>
          <div>
            <label className="block text-xs font-bold uppercase text-slate-700 mb-1">
              Refinement Instructions (Optional)
            </label>
            <textarea
              rows={3}
              value={regenInstructions}
              onChange={(e) => setRegenInstructions(e.target.value)}
              placeholder="e.g. Focus on practical SQL joins rather than theory."
              className="w-full px-3 py-2 border rounded-lg text-sm"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2 border-t">
            <button
              type="button"
              onClick={() => setRegeneratingQuestionId(null)}
              className="px-4 py-2 border rounded-lg text-xs font-semibold text-slate-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isRegenerating}
              className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-xs font-semibold disabled:opacity-50 flex items-center gap-1.5"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isRegenerating ? 'animate-spin' : ''}`} />
              <span>{isRegenerating ? 'Regenerating...' : 'Regenerate Question'}</span>
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
