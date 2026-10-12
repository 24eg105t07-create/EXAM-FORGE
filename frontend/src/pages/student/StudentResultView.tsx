import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { apiClient } from '../../api/client';
import { DetailedResult } from '../../types';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import {
  Award,
  CheckCircle2,
  XCircle,
  HelpCircle,
  Clock,
  ArrowLeft,
  BookOpen,
  BarChart3,
  Layers,
  Printer
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Cell
} from 'recharts';

export const StudentResultView: React.FC = () => {
  const { attemptId } = useParams<{ attemptId: string }>();
  const [result, setResult] = useState<DetailedResult | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<'all' | 'correct' | 'incorrect'>('all');

  useEffect(() => {
    loadDetailedResult();
  }, [attemptId]);

  const loadDetailedResult = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await apiClient.get<DetailedResult>(`/results/attempt/${attemptId}`);
      setResult(res.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load examination scorecard.');
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Generating scorecard and analytics..." />;
  }

  if (error || !result) {
    return (
      <div className="max-w-xl mx-auto my-12 p-6 bg-white rounded-xl border border-red-200 text-center space-y-3">
        <h2 className="text-lg font-bold text-slate-900">Scorecard Notice</h2>
        <p className="text-sm text-red-600">
          {error || 'This result is not yet released or you are not authorized to view it.'}
        </p>
        <Link
          to="/student"
          className="inline-flex items-center gap-1.5 px-4 py-2 bg-slate-800 text-white text-xs font-semibold rounded-lg hover:bg-slate-900"
        >
          <ArrowLeft className="w-4 h-4" /> Return to Student Dashboard
        </Link>
      </div>
    );
  }

  // Prepare topic chart data
  const topicData = Object.entries(result.topicBreakdown || {}).map(([topic, data]) => ({
    name: topic,
    percentage: Math.round(data.percentage),
    marksObtained: data.marksObtained,
    totalMarks: data.totalMarks,
  }));

  // Prepare difficulty chart data
  const difficultyData = Object.entries(result.difficultyBreakdown || {}).map(([diff, data]) => ({
    name: diff,
    percentage: Math.round(data.percentage),
    marksObtained: data.marksObtained,
    totalMarks: data.totalMarks,
  }));

  const filteredAnswers = result.answers.filter((ans) => {
    if (filter === 'correct') return ans.isCorrect === true;
    if (filter === 'incorrect') return ans.isCorrect === false;
    return true;
  });

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      {/* Top Navigation & Print */}
      <div className="flex items-center justify-between">
        <Link
          to="/student"
          className="inline-flex items-center gap-1.5 text-xs font-bold text-slate-600 hover:text-slate-900 transition"
        >
          <ArrowLeft className="w-4 h-4" /> Back to Dashboard
        </Link>
        <button
          onClick={() => window.print()}
          className="px-3.5 py-1.5 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 rounded-lg text-xs font-bold transition flex items-center gap-1.5 shadow-xs"
        >
          <Printer className="w-3.5 h-3.5" /> Print Scorecard
        </button>
      </div>

      {/* Main Scorecard Banner */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div>
          <span className="text-xs font-bold text-blue-700 bg-blue-50 px-2.5 py-1 rounded border border-blue-200">
            {result.subjectName}
          </span>
          <h1 className="text-2xl font-black text-slate-900 mt-2">{result.examTitle}</h1>
          <div className="flex items-center gap-3 text-xs text-slate-500 mt-1">
            <span>Student: <strong className="text-slate-800">{result.studentName}</strong></span>
            <span>•</span>
            <span>Roll: <strong className="text-slate-800">{result.rollNumber || 'N/A'}</strong></span>
            <span>•</span>
            <span>Evaluated: {new Date(result.evaluatedAt).toLocaleDateString()}</span>
          </div>
        </div>

        {/* Big Score Callout */}
        <div className="flex items-center gap-4 bg-slate-50 p-4 rounded-xl border border-slate-200 shrink-0">
          <div className="text-right">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
              FINAL SCORE
            </span>
            <div className="text-2xl font-black text-slate-900">
              {result.totalScore} <span className="text-sm font-semibold text-slate-400">/ {result.maxMarks}</span>
            </div>
            <span
              className={`text-xs font-bold px-2 py-0.5 rounded-full inline-block mt-0.5 ${
                result.passed ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'
              }`}
            >
              {result.percentage}% • {result.passed ? 'PASSED' : 'FAILED'}
            </span>
          </div>
          <div
            className={`w-12 h-12 rounded-xl flex items-center justify-center ${
              result.passed ? 'bg-emerald-500 text-white' : 'bg-rose-500 text-white'
            }`}
          >
            <Award className="w-7 h-7" />
          </div>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs text-center">
          <span className="text-xs text-slate-400 font-bold block">TOTAL QUESTIONS</span>
          <span className="text-xl font-black text-slate-900 mt-1 block">{result.totalQuestions}</span>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs text-center">
          <span className="text-xs text-emerald-600 font-bold block">CORRECT</span>
          <span className="text-xl font-black text-emerald-700 mt-1 block">{result.correctCount}</span>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs text-center">
          <span className="text-xs text-rose-600 font-bold block">INCORRECT</span>
          <span className="text-xl font-black text-rose-700 mt-1 block">{result.incorrectCount}</span>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs text-center">
          <span className="text-xs text-amber-600 font-bold block">UNANSWERED</span>
          <span className="text-xl font-black text-amber-700 mt-1 block">{result.unansweredCount}</span>
        </div>
      </div>

      {/* Analytics Charts */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Topic Breakdown */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs space-y-3">
          <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
            <BookOpen className="w-4 h-4 text-blue-600" /> Topic-Wise Mastery (%)
          </h3>
          <div className="h-48 w-full pt-2">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={topicData}>
                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} />
                <Tooltip formatter={(value: any) => [`${value}%`, 'Accuracy']} />
                <Bar dataKey="percentage" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Difficulty Breakdown */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs space-y-3">
          <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
            <Layers className="w-4 h-4 text-indigo-600" /> Difficulty-Wise Accuracy (%)
          </h3>
          <div className="h-48 w-full pt-2">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={difficultyData}>
                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} />
                <Tooltip formatter={(value: any) => [`${value}%`, 'Accuracy']} />
                <Bar dataKey="percentage" fill="#6366f1" radius={[4, 4, 0, 0]}>
                  {difficultyData.map((entry, index) => (
                    <Cell
                      key={`cell-${index}`}
                      fill={
                        entry.name === 'EASY'
                          ? '#10b981'
                          : entry.name === 'MEDIUM'
                          ? '#3b82f6'
                          : '#f59e0b'
                      }
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Question Details Header with Filter */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
          <div>
            <h2 className="text-base font-bold text-slate-900">Question Evaluation & Explanations</h2>
            <p className="text-xs text-slate-500">
              Review your answers alongside official answer keys and AI explanations.
            </p>
          </div>
          <div className="flex items-center gap-1.5 bg-slate-100 p-1 rounded-lg text-xs font-semibold">
            <button
              onClick={() => setFilter('all')}
              className={`px-3 py-1 rounded-md transition ${filter === 'all' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600'}`}
            >
              All ({result.answers.length})
            </button>
            <button
              onClick={() => setFilter('correct')}
              className={`px-3 py-1 rounded-md transition ${filter === 'correct' ? 'bg-white text-emerald-700 shadow-xs' : 'text-slate-600'}`}
            >
              Correct ({result.correctCount})
            </button>
            <button
              onClick={() => setFilter('incorrect')}
              className={`px-3 py-1 rounded-md transition ${filter === 'incorrect' ? 'bg-white text-rose-700 shadow-xs' : 'text-slate-600'}`}
            >
              Incorrect ({result.incorrectCount})
            </button>
          </div>
        </div>

        {/* Answers List */}
        <div className="space-y-4">
          {filteredAnswers.map((ans, idx) => (
            <div
              key={ans.questionId}
              className={`p-5 rounded-xl border text-xs space-y-3 transition ${
                ans.isCorrect
                  ? 'border-emerald-200 bg-emerald-50/20'
                  : 'border-rose-200 bg-rose-50/20'
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="font-mono font-bold text-slate-700 bg-slate-100 px-2 py-0.5 rounded">
                    Q{ans.questionOrder}
                  </span>
                  <span className="text-[10px] font-bold text-slate-600 bg-slate-100 px-2 py-0.5 rounded">
                    {ans.lessonTitle}
                  </span>
                  <span className="text-[10px] font-bold text-slate-500 uppercase">
                    {ans.difficulty}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="font-bold text-slate-700">
                    Marks: <strong className={ans.isCorrect ? 'text-emerald-700' : 'text-rose-700'}>{ans.marksAwarded}</strong> / {ans.maxMarks}
                  </span>
                  <span
                    className={`px-2 py-0.5 rounded-md font-bold flex items-center gap-1 ${
                      ans.isCorrect ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'
                    }`}
                  >
                    {ans.isCorrect ? (
                      <>
                        <CheckCircle2 className="w-3.5 h-3.5" /> Correct
                      </>
                    ) : (
                      <>
                        <XCircle className="w-3.5 h-3.5" /> Incorrect
                      </>
                    )}
                  </span>
                </div>
              </div>

              {/* Question Text */}
              <div className="text-sm font-semibold text-slate-900 leading-relaxed">
                {ans.questionText}
              </div>

              {/* Student Answer vs Correct Answer */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-1">
                <div className="p-3 bg-white rounded-lg border border-slate-200">
                  <span className="text-[10px] font-bold text-slate-400 uppercase block mb-1">
                    Your Answer
                  </span>
                  {ans.selectedOptionLabel ? (
                    <div className="text-slate-800 font-medium">
                      <strong className="text-blue-600">[{ans.selectedOptionLabel}]</strong> {ans.selectedOptionText}
                    </div>
                  ) : ans.studentTextAnswer ? (
                    <div className="text-slate-800">{ans.studentTextAnswer}</div>
                  ) : (
                    <span className="text-slate-400 italic">Not attempted (Unanswered)</span>
                  )}
                </div>

                <div className="p-3 bg-white rounded-lg border border-slate-200">
                  <span className="text-[10px] font-bold text-emerald-600 uppercase block mb-1">
                    Correct Answer
                  </span>
                  {ans.correctOptionLabel ? (
                    <div className="text-emerald-800 font-medium">
                      <strong>[{ans.correctOptionLabel}]</strong> {ans.correctOptionText}
                    </div>
                  ) : (
                    <span className="text-slate-400 italic">Evaluated by Instructor</span>
                  )}
                </div>
              </div>

              {/* Explanation & Source Reference */}
              {ans.explanation && (
                <div className="p-3 bg-indigo-50/60 rounded-lg border border-indigo-100 text-indigo-950 text-xs leading-relaxed">
                  <strong className="block font-bold text-indigo-900 mb-0.5">Explanation:</strong>
                  {ans.explanation}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
