import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { apiClient } from '../../api/client';
import { Subject, Lesson, ExamBlueprint } from '../../types';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Sparkles, Sliders, CheckCircle2, AlertCircle, ArrowRight } from 'lucide-react';

export const BlueprintBuilderPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialSubjectId = searchParams.get('subjectId');

  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | ''>(
    initialSubjectId ? Number(initialSubjectId) : ''
  );
  const [lessons, setLessons] = useState<Lesson[]>([]);
  const [blueprints, setBlueprints] = useState<ExamBlueprint[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Blueprint Form
  const [title, setTitle] = useState('DBMS Mid-Semester Examination 2026');
  const [totalQuestions, setTotalQuestions] = useState<number>(20);
  const [durationMinutes, setDurationMinutes] = useState<number>(60);
  const [maxMarks, setMaxMarks] = useState<number>(20);
  const [easyPct, setEasyPct] = useState<number>(30);
  const [mediumPct, setMediumPct] = useState<number>(50);
  const [hardPct, setHardPct] = useState<number>(20);

  const [mcqCount, setMcqCount] = useState<number>(14);
  const [tfCount, setTfCount] = useState<number>(4);
  const [saCount, setSaCount] = useState<number>(2);

  const [instructions, setInstructions] = useState(
    'Answer all objective questions. Focus on SQL queries and Transaction ACID isolation levels.'
  );

  // Lesson distribution
  const [lessonPercentages, setLessonPercentages] = useState<Record<number, number>>({});
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (selectedSubjectId) {
      loadLessonsForSubject(Number(selectedSubjectId));
    }
  }, [selectedSubjectId]);

  const loadInitialData = async () => {
    try {
      const subRes = await apiClient.get<Subject[]>('/subjects');
      setSubjects(subRes.data);
      if (subRes.data.length > 0 && !selectedSubjectId) {
        setSelectedSubjectId(subRes.data[0].id);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const loadLessonsForSubject = async (subId: number) => {
    try {
      const [lessonRes, bpRes] = await Promise.all([
        apiClient.get<Lesson[]>(`/teacher/lessons?subjectId=${subId}`),
        apiClient.get<ExamBlueprint[]>(`/teacher/blueprints?subjectId=${subId}`),
      ]);
      setLessons(lessonRes.data);
      setBlueprints(bpRes.data);

      // Distribute evenly among available lessons by default
      if (lessonRes.data.length > 0) {
        const count = lessonRes.data.length;
        const perLesson = Math.floor(100 / count);
        const map: Record<number, number> = {};
        lessonRes.data.forEach((l, idx) => {
          map[l.id] = idx === count - 1 ? 100 - perLesson * (count - 1) : perLesson;
        });
        setLessonPercentages(map);
      }
    } catch (err) {
      console.error(err);
    }
  };

  // Calculations
  const diffSum = easyPct + mediumPct + hardPct;
  const isDiffValid = diffSum === 100;

  const easyCountCalc = Math.round((totalQuestions * easyPct) / 100);
  const medCountCalc = Math.round((totalQuestions * mediumPct) / 100);
  const hardCountCalc = Math.max(0, totalQuestions - easyCountCalc - medCountCalc);

  const qTypeSum = mcqCount + tfCount + saCount;
  const isQTypeValid = qTypeSum === totalQuestions;

  const lessonSum = Object.values(lessonPercentages).reduce((a, b) => a + b, 0);
  const isLessonValid = lessonSum === 100;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isDiffValid) {
      setError(`Difficulty percentages must sum to 100% (currently ${diffSum}%).`);
      return;
    }
    if (!isQTypeValid) {
      setError(`Question type counts (${qTypeSum}) must equal total questions (${totalQuestions}).`);
      return;
    }
    if (!isLessonValid) {
      setError(`Lesson distribution percentages must sum to 100% (currently ${lessonSum}%).`);
      return;
    }

    setError(null);
    setIsSaving(true);

    const lessonSelections = Object.entries(lessonPercentages).map(([lessonId, targetPercentage]) => ({
      lessonId: Number(lessonId),
      targetPercentage,
    }));

    try {
      const res = await apiClient.post<ExamBlueprint>('/teacher/blueprints', {
        subjectId: Number(selectedSubjectId),
        title,
        totalQuestions,
        durationMinutes,
        maxMarks,
        easyPercentage: easyPct,
        mediumPercentage: mediumPct,
        hardPercentage: hardPct,
        mcqCount,
        trueFalseCount: tfCount,
        shortAnswerCount: saCount,
        marksPerQuestion: 1.0,
        instructions,
        lessonSelections,
      });

      // Navigate straight to generator with created blueprint
      navigate(`/teacher/generator?blueprintId=${res.data.id}`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create exam blueprint');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading blueprint designer..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Sliders className="w-6 h-6 text-blue-600" />
            Exam Blueprint Architect
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Specify deterministic difficulty splits, curriculum unit weights, and format breakdown for AI synthesis.
          </p>
        </div>
      </div>

      {error && (
        <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Form Controls */}
        <div className="lg:col-span-2 space-y-6">
          {/* General Parameters */}
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs space-y-4">
            <h3 className="text-sm font-bold uppercase text-slate-800 tracking-wider">
              1. General Examination Setup
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold uppercase text-slate-600 mb-1">Subject</label>
                <select
                  required
                  value={selectedSubjectId}
                  onChange={(e) => setSelectedSubjectId(Number(e.target.value))}
                  className="w-full px-3 py-2 border rounded-lg text-sm bg-white"
                >
                  {subjects.map((s) => (
                    <option key={s.id} value={s.id}>
                      [{s.code}] {s.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase text-slate-600 mb-1">Exam Title</label>
                <input
                  type="text"
                  required
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="w-full px-3 py-2 border rounded-lg text-sm"
                />
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-bold uppercase text-slate-600 mb-1">Total Questions</label>
                <input
                  type="number"
                  required
                  min={1}
                  max={100}
                  value={totalQuestions}
                  onChange={(e) => setTotalQuestions(Number(e.target.value))}
                  className="w-full px-3 py-2 border rounded-lg text-sm font-bold text-blue-700"
                />
              </div>
              <div>
                <label className="block text-xs font-bold uppercase text-slate-600 mb-1">Duration (Min)</label>
                <input
                  type="number"
                  required
                  min={5}
                  value={durationMinutes}
                  onChange={(e) => setDurationMinutes(Number(e.target.value))}
                  className="w-full px-3 py-2 border rounded-lg text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-bold uppercase text-slate-600 mb-1">Max Marks</label>
                <input
                  type="number"
                  required
                  value={maxMarks}
                  onChange={(e) => setMaxMarks(Number(e.target.value))}
                  className="w-full px-3 py-2 border rounded-lg text-sm"
                />
              </div>
            </div>
          </div>

          {/* Difficulty Sliders */}
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold uppercase text-slate-800 tracking-wider">
                2. Difficulty Distribution (Must Sum to 100%)
              </h3>
              <span
                className={`text-xs font-bold px-2.5 py-0.5 rounded-full border ${
                  isDiffValid
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-rose-50 text-rose-700 border-rose-200'
                }`}
              >
                Sum: {diffSum}%
              </span>
            </div>

            <div className="space-y-4">
              <div>
                <div className="flex justify-between text-xs font-semibold mb-1">
                  <span className="text-emerald-700">Easy: {easyPct}% ({easyCountCalc} questions)</span>
                  <input
                    type="number"
                    min={0}
                    max={100}
                    value={easyPct}
                    onChange={(e) => setEasyPct(Number(e.target.value))}
                    className="w-16 px-1.5 py-0.5 border rounded text-right text-xs"
                  />
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={easyPct}
                  onChange={(e) => setEasyPct(Number(e.target.value))}
                  className="w-full accent-emerald-600 cursor-pointer"
                />
              </div>

              <div>
                <div className="flex justify-between text-xs font-semibold mb-1">
                  <span className="text-blue-700">Medium: {mediumPct}% ({medCountCalc} questions)</span>
                  <input
                    type="number"
                    min={0}
                    max={100}
                    value={mediumPct}
                    onChange={(e) => setMediumPct(Number(e.target.value))}
                    className="w-16 px-1.5 py-0.5 border rounded text-right text-xs"
                  />
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={mediumPct}
                  onChange={(e) => setMediumPct(Number(e.target.value))}
                  className="w-full accent-blue-600 cursor-pointer"
                />
              </div>

              <div>
                <div className="flex justify-between text-xs font-semibold mb-1">
                  <span className="text-purple-700">Hard: {hardPct}% ({hardCountCalc} questions)</span>
                  <input
                    type="number"
                    min={0}
                    max={100}
                    value={hardPct}
                    onChange={(e) => setHardPct(Number(e.target.value))}
                    className="w-16 px-1.5 py-0.5 border rounded text-right text-xs"
                  />
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={hardPct}
                  onChange={(e) => setHardPct(Number(e.target.value))}
                  className="w-full accent-purple-600 cursor-pointer"
                />
              </div>
            </div>
          </div>

          {/* Question Formats */}
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold uppercase text-slate-800 tracking-wider">
                3. Question Format Breakdown
              </h3>
              <span
                className={`text-xs font-bold px-2.5 py-0.5 rounded-full border ${
                  isQTypeValid
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-rose-50 text-rose-700 border-rose-200'
                }`}
              >
                Sum: {qTypeSum} / {totalQuestions}
              </span>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Multiple Choice (MCQ)</label>
                <input
                  type="number"
                  min={0}
                  value={mcqCount}
                  onChange={(e) => setMcqCount(Number(e.target.value))}
                  className="w-full px-3 py-2 border rounded-lg text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">True / False</label>
                <input
                  type="number"
                  min={0}
                  value={tfCount}
                  onChange={(e) => setTfCount(Number(e.target.value))}
                  className="w-full px-3 py-2 border rounded-lg text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Short Answer</label>
                <input
                  type="number"
                  min={0}
                  value={saCount}
                  onChange={(e) => setSaCount(Number(e.target.value))}
                  className="w-full px-3 py-2 border rounded-lg text-sm"
                />
              </div>
            </div>
          </div>

          {/* Lesson Weights */}
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold uppercase text-slate-800 tracking-wider">
                4. Topic / Lesson Distribution (Must Sum to 100%)
              </h3>
              <span
                className={`text-xs font-bold px-2.5 py-0.5 rounded-full border ${
                  isLessonValid
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-rose-50 text-rose-700 border-rose-200'
                }`}
              >
                Sum: {lessonSum}%
              </span>
            </div>

            {lessons.length === 0 ? (
              <p className="text-xs text-amber-700 bg-amber-50 p-3 rounded-lg">
                No units created for this subject yet. Please add lesson units in the Lesson Materials tab first.
              </p>
            ) : (
              <div className="space-y-3">
                {lessons.map((l) => (
                  <div key={l.id} className="flex items-center justify-between gap-4 p-2.5 bg-slate-50 rounded-lg border border-slate-200">
                    <div className="text-xs">
                      <span className="font-bold text-slate-900">Unit {l.unitNumber}: {l.title}</span>
                      <span className="text-slate-500 block text-[11px]">
                        Target: {Math.round((totalQuestions * (lessonPercentages[l.id] || 0)) / 100)} questions
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <input
                        type="number"
                        min={0}
                        max={100}
                        value={lessonPercentages[l.id] || 0}
                        onChange={(e) =>
                          setLessonPercentages({
                            ...lessonPercentages,
                            [l.id]: Number(e.target.value),
                          })
                        }
                        className="w-16 px-2 py-1 border rounded text-right text-xs font-bold"
                      />
                      <span className="text-xs font-semibold text-slate-500">%</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right Col: Blueprint Summary & Trigger */}
        <div className="space-y-6">
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs space-y-4 sticky top-20">
            <h3 className="text-sm font-bold uppercase text-slate-800 tracking-wider">
              Blueprint Verification Matrix
            </h3>

            <div className="divide-y divide-slate-100 text-xs">
              <div className="py-2 flex justify-between">
                <span className="text-slate-500">Total Questions</span>
                <span className="font-bold text-slate-900">{totalQuestions}</span>
              </div>
              <div className="py-2 flex justify-between">
                <span className="text-slate-500">Easy Questions</span>
                <span className="font-bold text-emerald-700">{easyCountCalc} ({easyPct}%)</span>
              </div>
              <div className="py-2 flex justify-between">
                <span className="text-slate-500">Medium Questions</span>
                <span className="font-bold text-blue-700">{medCountCalc} ({mediumPct}%)</span>
              </div>
              <div className="py-2 flex justify-between">
                <span className="text-slate-500">Hard Questions</span>
                <span className="font-bold text-purple-700">{hardCountCalc} ({hardPct}%)</span>
              </div>
              <div className="py-2 flex justify-between">
                <span className="text-slate-500">MCQ Format</span>
                <span className="font-bold text-slate-800">{mcqCount}</span>
              </div>
              <div className="py-2 flex justify-between">
                <span className="text-slate-500">True/False Format</span>
                <span className="font-bold text-slate-800">{tfCount}</span>
              </div>
              <div className="py-2 flex justify-between">
                <span className="text-slate-500">Short Answer Format</span>
                <span className="font-bold text-slate-800">{saCount}</span>
              </div>
            </div>

            <button
              type="submit"
              disabled={isSaving || !isDiffValid || !isQTypeValid || !isLessonValid || lessons.length === 0}
              className="w-full py-3 px-4 bg-linear-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white rounded-xl text-sm font-bold shadow-md shadow-blue-500/20 transition flex items-center justify-center gap-2 disabled:opacity-50"
            >
              {isSaving ? (
                <LoadingSpinner size="sm" />
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  <span>Save & Generate Questions</span>
                </>
              )}
            </button>
          </div>

          {/* Past Blueprints */}
          {blueprints.length > 0 && (
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
              <h4 className="text-xs font-bold uppercase text-slate-500 mb-3">Existing Blueprints</h4>
              <div className="space-y-2">
                {blueprints.map((bp) => (
                  <div key={bp.id} className="p-3 bg-slate-50 rounded-lg border border-slate-200 text-xs">
                    <div className="font-bold text-slate-800">{bp.title}</div>
                    <div className="text-[11px] text-slate-500 mt-0.5">
                      {bp.totalQuestions} Questions • E:{bp.easyPercentage}% M:{bp.mediumPercentage}% H:{bp.hardPercentage}%
                    </div>
                    <button
                      type="button"
                      onClick={() => navigate(`/teacher/generator?blueprintId=${bp.id}`)}
                      className="mt-2 text-blue-600 hover:text-blue-800 font-semibold flex items-center gap-1 text-[11px]"
                    >
                      Use to Generate Paper <ArrowRight className="w-3 h-3" />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </form>
    </div>
  );
};
