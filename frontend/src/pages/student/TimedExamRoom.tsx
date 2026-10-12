import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiClient } from '../../api/client';
import {
  StudentExamSession,
  StudentQuestion,
  SavedAnswer
} from '../../types';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Modal } from '../../components/common/Modal';
import {
  Clock,
  CheckCircle2,
  AlertCircle,
  Bookmark,
  ChevronLeft,
  ChevronRight,
  Send,
  HelpCircle,
  RotateCcw,
  Shield,
  FileCheck2
} from 'lucide-react';

export const TimedExamRoom: React.FC = () => {
  const { scheduleId } = useParams<{ scheduleId: string }>();
  const navigate = useNavigate();

  const [session, setSession] = useState<StudentExamSession | null>(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<number, { selectedOptionId?: number; textAnswer?: string; isMarkedForReview: boolean }>>({});
  const [remainingSecs, setRemainingSecs] = useState<number>(0);
  const [savingStatus, setSavingStatus] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showSubmitModal, setShowSubmitModal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitResult, setSubmitResult] = useState<{ attemptId: number; message: string; score?: number; maxMarks?: number } | null>(null);

  const timerRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    initExamSession();
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [scheduleId]);

  const initExamSession = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await apiClient.post<StudentExamSession>(`/student/exams/${scheduleId}/start`);
      const s = res.data;
      setSession(s);
      setRemainingSecs(s.remainingSeconds);

      // Pre-fill answers from saved state
      const initialMap: Record<number, { selectedOptionId?: number; textAnswer?: string; isMarkedForReview: boolean }> = {};
      if (s.savedAnswers) {
        Object.entries(s.savedAnswers).forEach(([qid, ans]) => {
          initialMap[Number(qid)] = {
            selectedOptionId: ans.selectedOptionId,
            textAnswer: ans.textAnswer,
            isMarkedForReview: ans.isMarkedForReview || false,
          };
        });
      }
      setAnswers(initialMap);

      // Start countdown timer
      startTimer(s.remainingSeconds);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to start examination session.');
    } finally {
      setIsLoading(false);
    }
  };

  const startTimer = (seconds: number) => {
    if (timerRef.current) clearInterval(timerRef.current);
    let current = seconds;

    timerRef.current = setInterval(() => {
      current -= 1;
      setRemainingSecs(current);

      if (current <= 0) {
        if (timerRef.current) clearInterval(timerRef.current);
        handleAutoSubmitOnTimeout();
      }
    }, 1000);
  };

  const handleAutoSubmitOnTimeout = async () => {
    alert('Time has expired! Submitting your examination answers to the server...');
    await executeFinalSubmit();
  };

  const currentQuestion: StudentQuestion | undefined = session?.questions[currentIndex];

  const handleSelectOption = async (optionId: number) => {
    if (!currentQuestion) return;
    const qid = currentQuestion.questionId;
    const prev = answers[qid] || { isMarkedForReview: false };

    const updated = {
      ...prev,
      selectedOptionId: optionId,
    };

    setAnswers((prevAll) => ({
      ...prevAll,
      [qid]: updated,
    }));

    await syncAnswerToServer(qid, updated.selectedOptionId, updated.textAnswer, updated.isMarkedForReview);
  };

  const handleTextAnswerChange = async (text: string) => {
    if (!currentQuestion) return;
    const qid = currentQuestion.questionId;
    const prev = answers[qid] || { isMarkedForReview: false };

    const updated = {
      ...prev,
      textAnswer: text,
    };

    setAnswers((prevAll) => ({
      ...prevAll,
      [qid]: updated,
    }));

    await syncAnswerToServer(qid, updated.selectedOptionId, updated.textAnswer, updated.isMarkedForReview);
  };

  const handleToggleReview = async () => {
    if (!currentQuestion) return;
    const qid = currentQuestion.questionId;
    const prev = answers[qid] || { isMarkedForReview: false };

    const updated = {
      ...prev,
      isMarkedForReview: !prev.isMarkedForReview,
    };

    setAnswers((prevAll) => ({
      ...prevAll,
      [qid]: updated,
    }));

    await syncAnswerToServer(qid, updated.selectedOptionId, updated.textAnswer, updated.isMarkedForReview);
  };

  const handleClearSelection = async () => {
    if (!currentQuestion) return;
    const qid = currentQuestion.questionId;
    const prev = answers[qid] || { isMarkedForReview: false };

    const updated = {
      selectedOptionId: undefined,
      textAnswer: undefined,
      isMarkedForReview: prev.isMarkedForReview,
    };

    setAnswers((prevAll) => ({
      ...prevAll,
      [qid]: updated,
    }));

    await syncAnswerToServer(qid, undefined, undefined, updated.isMarkedForReview);
  };

  const syncAnswerToServer = async (
    questionId: number,
    selectedOptionId?: number,
    textAnswer?: string,
    isMarkedForReview?: boolean
  ) => {
    setSavingStatus('saving');
    try {
      await apiClient.post(`/student/exams/${scheduleId}/save-answer`, {
        questionId,
        selectedOptionId: selectedOptionId ?? null,
        textAnswer: textAnswer ?? null,
        isMarkedForReview: isMarkedForReview ?? false,
      });
      setSavingStatus('saved');
      setTimeout(() => setSavingStatus('idle'), 2000);
    } catch (err) {
      console.error('Failed to autosave answer', err);
      setSavingStatus('error');
    }
  };

  const executeFinalSubmit = async () => {
    setIsSubmitting(true);
    try {
      if (timerRef.current) clearInterval(timerRef.current);
      const res = await apiClient.post(`/student/exams/${scheduleId}/submit`);
      setShowSubmitModal(false);
      setSubmitResult({
        attemptId: res.data.attemptId,
        message: res.data.message || 'Exam submitted successfully.',
        score: res.data.score,
        maxMarks: res.data.maxMarks,
      });
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to submit examination.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatTimer = (totalSeconds: number) => {
    if (totalSeconds <= 0) return '00:00:00';
    const hrs = Math.floor(totalSeconds / 3600);
    const mins = Math.floor((totalSeconds % 3600) / 60);
    const secs = totalSeconds % 60;
    return `${hrs.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  if (isLoading) {
    return <LoadingSpinner message="Entering proctored examination environment..." />;
  }

  if (error || !session) {
    return (
      <div className="max-w-xl mx-auto my-12 p-6 bg-white rounded-xl border border-red-200 text-center space-y-3">
        <AlertCircle className="w-12 h-12 text-red-500 mx-auto" />
        <h2 className="text-lg font-bold text-slate-900">Session Error</h2>
        <p className="text-sm text-red-600">{error || 'Could not load examination session.'}</p>
        <button
          onClick={() => navigate('/student')}
          className="px-4 py-2 bg-slate-800 text-white text-xs font-semibold rounded-lg hover:bg-slate-900 transition"
        >
          Return to Dashboard
        </button>
      </div>
    );
  }

  // Submission Done Screen
  if (submitResult) {
    return (
      <div className="max-w-2xl mx-auto my-12 p-8 bg-white rounded-2xl border border-slate-200 shadow-sm text-center space-y-5">
        <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto">
          <FileCheck2 className="w-9 h-9" />
        </div>
        <h2 className="text-2xl font-black text-slate-900">Examination Submitted Successfully!</h2>
        <p className="text-sm text-slate-600">
          Your responses for <span className="font-bold text-slate-800">{session.examTitle}</span> have been recorded
          and securely evaluated by ExamForge.
        </p>

        {submitResult.score !== undefined && submitResult.score !== null ? (
          <div className="p-4 bg-emerald-50 rounded-xl border border-emerald-200 inline-block text-left min-w-[280px]">
            <span className="text-xs font-bold text-emerald-800 uppercase block">Instant Result Release</span>
            <div className="text-2xl font-black text-emerald-700 mt-1">
              Score: {submitResult.score} / {submitResult.maxMarks}
            </div>
          </div>
        ) : (
          <div className="p-4 bg-blue-50 rounded-xl border border-blue-200 text-xs text-blue-800 max-w-md mx-auto">
            Results for this examination are scheduled for institutional release after the completion window closes.
          </div>
        )}

        <div className="pt-4 flex items-center justify-center gap-3">
          <button
            onClick={() => navigate('/student')}
            className="px-5 py-2.5 bg-slate-800 hover:bg-slate-900 text-white rounded-lg text-xs font-bold transition"
          >
            Back to Dashboard
          </button>
          <button
            onClick={() => navigate(`/student/results/${submitResult.attemptId}`)}
            className="px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-bold transition"
          >
            View Result & Details
          </button>
        </div>
      </div>
    );
  }

  // Calculations for summary palette
  const totalCount = session.questions.length;
  const answeredCount = session.questions.filter((q) => {
    const a = answers[q.questionId];
    return a && (a.selectedOptionId !== undefined || (a.textAnswer && a.textAnswer.trim().length > 0));
  }).length;
  const reviewCount = session.questions.filter((q) => answers[q.questionId]?.isMarkedForReview).length;
  const unansweredCount = totalCount - answeredCount;

  const currentAnswer = currentQuestion ? answers[currentQuestion.questionId] : undefined;
  const isUrgentTimer = remainingSecs < 300; // less than 5 minutes

  return (
    <div className="max-w-7xl mx-auto -mt-2 space-y-4">
      {/* Exam Header Bar */}
      <header className="bg-white px-6 py-4 rounded-2xl border border-slate-200 shadow-xs flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <span className="text-[11px] font-bold text-blue-700 bg-blue-50 px-2 py-0.5 rounded border border-blue-200">
            {session.subjectName}
          </span>
          <h1 className="text-lg font-black text-slate-900 mt-1">{session.examTitle}</h1>
        </div>

        <div className="flex items-center gap-4 w-full md:w-auto justify-between md:justify-end">
          {/* Autosave Status */}
          <div className="flex items-center gap-1.5 text-xs text-slate-500 font-medium">
            {savingStatus === 'saving' && (
              <span className="text-blue-600 flex items-center gap-1">
                <LoadingSpinner size="sm" /> Autosaving...
              </span>
            )}
            {savingStatus === 'saved' && (
              <span className="text-emerald-600 flex items-center gap-1">
                <CheckCircle2 className="w-3.5 h-3.5" /> All Saved
              </span>
            )}
            {savingStatus === 'error' && (
              <span className="text-rose-600 flex items-center gap-1">
                <AlertCircle className="w-3.5 h-3.5" /> Save issue
              </span>
            )}
          </div>

          {/* Countdown Timer */}
          <div
            className={`px-4 py-2 rounded-xl border flex items-center gap-2 font-mono font-bold text-sm tracking-wider shadow-xs ${
              isUrgentTimer
                ? 'bg-rose-50 border-rose-300 text-rose-700 animate-pulse'
                : 'bg-slate-900 border-slate-800 text-white'
            }`}
          >
            <Clock className={`w-4 h-4 ${isUrgentTimer ? 'text-rose-600' : 'text-blue-400'}`} />
            <span>{formatTimer(remainingSecs)}</span>
          </div>

          {/* Submit Exam Button */}
          <button
            onClick={() => setShowSubmitModal(true)}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition flex items-center gap-1.5 shadow-xs"
          >
            <Send className="w-3.5 h-3.5" />
            <span>Finish & Submit</span>
          </button>
        </div>
      </header>

      {/* Main Examination Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
        {/* Question Content View (3 Columns) */}
        <div className="lg:col-span-3 bg-white p-6 rounded-2xl border border-slate-200 shadow-xs flex flex-col justify-between min-h-[520px]">
          {currentQuestion ? (
            <div className="space-y-6">
              {/* Question Meta Bar */}
              <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-black text-slate-800 bg-slate-100 px-3 py-1 rounded-lg">
                    Question {currentIndex + 1} of {totalCount}
                  </span>
                  <span className="text-xs font-bold text-indigo-700 bg-indigo-50 px-2.5 py-1 rounded-lg border border-indigo-200">
                    {currentQuestion.questionType.replace('_', ' ')}
                  </span>
                </div>
                <div className="text-xs font-bold text-slate-600">
                  Marks: <span className="text-emerald-700">{currentQuestion.marks}</span>
                </div>
              </div>

              {/* Question Text */}
              <div className="text-base font-semibold text-slate-900 leading-relaxed whitespace-pre-wrap">
                {currentQuestion.questionText}
              </div>

              {/* Options or Answer Input */}
              {currentQuestion.questionType === 'SHORT_ANSWER' ? (
                <div className="space-y-2 pt-2">
                  <label className="text-xs font-bold text-slate-700 block">Your Answer:</label>
                  <textarea
                    rows={6}
                    value={currentAnswer?.textAnswer || ''}
                    onChange={(e) => handleTextAnswerChange(e.target.value)}
                    placeholder="Type your response here..."
                    className="w-full text-sm p-3.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500 font-sans"
                  />
                </div>
              ) : (
                <div className="space-y-2.5 pt-2">
                  {currentQuestion.options.map((opt) => {
                    const isSelected = currentAnswer?.selectedOptionId === opt.id;
                    return (
                      <div
                        key={opt.id}
                        onClick={() => handleSelectOption(opt.id)}
                        className={`p-3.5 rounded-xl border cursor-pointer transition flex items-start gap-3 ${
                          isSelected
                            ? 'bg-blue-50/80 border-blue-500 text-blue-950 font-medium ring-1 ring-blue-500'
                            : 'bg-white border-slate-200 hover:bg-slate-50/70 text-slate-800'
                        }`}
                      >
                        <div
                          className={`w-6 h-6 rounded-lg text-xs font-bold flex items-center justify-center shrink-0 mt-0.5 ${
                            isSelected ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-600'
                          }`}
                        >
                          {opt.optionLabel}
                        </div>
                        <span className="text-sm leading-relaxed">{opt.optionText}</span>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          ) : (
            <div className="text-center py-12 text-slate-400">No question selected.</div>
          )}

          {/* Bottom Action Controls */}
          <div className="pt-6 border-t border-slate-100 flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <button
                onClick={handleToggleReview}
                className={`px-3 py-2 rounded-lg text-xs font-bold transition flex items-center gap-1.5 ${
                  currentAnswer?.isMarkedForReview
                    ? 'bg-amber-100 text-amber-800 border border-amber-300'
                    : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
                }`}
              >
                <Bookmark className="w-3.5 h-3.5" />
                <span>{currentAnswer?.isMarkedForReview ? 'Marked for Review' : 'Mark for Review'}</span>
              </button>

              <button
                onClick={handleClearSelection}
                className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-lg text-xs font-medium transition flex items-center gap-1"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Clear Selection</span>
              </button>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => setCurrentIndex((prev) => Math.max(0, prev - 1))}
                disabled={currentIndex === 0}
                className="px-3.5 py-2 bg-slate-100 hover:bg-slate-200 disabled:opacity-40 text-slate-700 rounded-lg text-xs font-bold transition flex items-center gap-1"
              >
                <ChevronLeft className="w-4 h-4" />
                <span>Previous</span>
              </button>

              <button
                onClick={() => setCurrentIndex((prev) => Math.min(totalCount - 1, prev + 1))}
                disabled={currentIndex === totalCount - 1}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-40 text-white rounded-lg text-xs font-bold transition flex items-center gap-1 shadow-xs"
              >
                <span>Next</span>
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>

        {/* Right Sidebar: Question Palette & Overview (1 Column) */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex flex-col justify-between space-y-4">
          <div>
            <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider mb-3">
              Question Palette
            </h3>

            {/* Quick summary metrics */}
            <div className="grid grid-cols-3 gap-2 mb-4 text-center">
              <div className="p-2 bg-emerald-50 rounded-lg border border-emerald-200">
                <span className="text-[10px] text-emerald-700 font-bold block">Answered</span>
                <span className="text-sm font-black text-emerald-800">{answeredCount}</span>
              </div>
              <div className="p-2 bg-amber-50 rounded-lg border border-amber-200">
                <span className="text-[10px] text-amber-700 font-bold block">Review</span>
                <span className="text-sm font-black text-amber-800">{reviewCount}</span>
              </div>
              <div className="p-2 bg-slate-50 rounded-lg border border-slate-200">
                <span className="text-[10px] text-slate-500 font-bold block">Left</span>
                <span className="text-sm font-black text-slate-700">{unansweredCount}</span>
              </div>
            </div>

            {/* Question numbers grid */}
            <div className="grid grid-cols-5 gap-2 max-h-[300px] overflow-y-auto pr-1">
              {session.questions.map((q, idx) => {
                const ans = answers[q.questionId];
                const isAnswered = ans && (ans.selectedOptionId !== undefined || (ans.textAnswer && ans.textAnswer.trim().length > 0));
                const isReview = ans?.isMarkedForReview;
                const isCurrent = idx === currentIndex;

                let btnClass = 'bg-slate-100 text-slate-700 hover:bg-slate-200';
                if (isAnswered && isReview) {
                  btnClass = 'bg-amber-500 text-white font-bold ring-2 ring-emerald-500';
                } else if (isReview) {
                  btnClass = 'bg-amber-500 text-white font-bold';
                } else if (isAnswered) {
                  btnClass = 'bg-emerald-600 text-white font-bold';
                }

                if (isCurrent) {
                  btnClass += ' ring-2 ring-blue-600 ring-offset-2';
                }

                return (
                  <button
                    key={q.questionId}
                    onClick={() => setCurrentIndex(idx)}
                    className={`h-9 rounded-lg text-xs transition flex items-center justify-center font-mono ${btnClass}`}
                  >
                    {idx + 1}
                  </button>
                );
              })}
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 space-y-2 text-[11px] text-slate-500">
            <div className="flex items-center gap-1.5">
              <Shield className="w-3.5 h-3.5 text-blue-600" />
              <span>Proctored Session ID: #{session.attemptId}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
              <span>Autosave active across all inputs</span>
            </div>
          </div>
        </div>
      </div>

      {/* Confirmation Submit Modal */}
      <Modal
        isOpen={showSubmitModal}
        onClose={() => setShowSubmitModal(false)}
        title="Finalize & Submit Examination"
        size="md"
      >
        <div className="space-y-4">
          <p className="text-xs text-slate-600 leading-relaxed">
            Are you sure you are ready to submit your answers? Once submitted, your examination attempt will be closed and locked from further edits.
          </p>

          <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 grid grid-cols-3 gap-3 text-center">
            <div>
              <span className="text-xs text-slate-500 block">Total</span>
              <span className="text-base font-bold text-slate-900">{totalCount}</span>
            </div>
            <div>
              <span className="text-xs text-emerald-600 font-bold block">Answered</span>
              <span className="text-base font-bold text-emerald-700">{answeredCount}</span>
            </div>
            <div>
              <span className="text-xs text-amber-600 font-bold block">Unanswered</span>
              <span className="text-base font-bold text-amber-700">{unansweredCount}</span>
            </div>
          </div>

          {unansweredCount > 0 && (
            <div className="p-3 bg-amber-50 rounded-lg border border-amber-200 text-xs text-amber-800 flex items-start gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 text-amber-600 mt-0.5" />
              <span>
                You still have {unansweredCount} unanswered questions. They will be marked with 0 marks if submitted now.
              </span>
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-3">
            <button
              onClick={() => setShowSubmitModal(false)}
              className="px-4 py-2 border border-slate-300 text-slate-700 rounded-lg text-xs font-semibold hover:bg-slate-50"
            >
              Continue Test
            </button>
            <button
              onClick={executeFinalSubmit}
              disabled={isSubmitting}
              className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold flex items-center gap-2"
            >
              {isSubmitting ? (
                <>
                  <LoadingSpinner size="sm" />
                  <span>Submitting Answers...</span>
                </>
              ) : (
                <>
                  <Send className="w-3.5 h-3.5" />
                  <span>Yes, Submit Examination</span>
                </>
              )}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
