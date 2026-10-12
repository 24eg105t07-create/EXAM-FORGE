import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { apiClient } from '../../api/client';
import { Subject, ExamBlueprint, ExamPaper } from '../../types';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Sparkles, Brain, CheckCircle2, AlertCircle, ArrowRight, BookOpen } from 'lucide-react';

export const AiQuestionGeneratorPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const urlBlueprintId = searchParams.get('blueprintId');

  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | ''>('');
  const [blueprints, setBlueprints] = useState<ExamBlueprint[]>([]);
  const [selectedBlueprintId, setSelectedBlueprintId] = useState<number | ''>(
    urlBlueprintId ? Number(urlBlueprintId) : ''
  );

  const [instructions, setInstructions] = useState(
    'Focus on core conceptual understanding from the uploaded lesson notes. Emphasize SQL group by aggregations, 2NF/3NF dependencies, and ACID transaction isolation guarantees.'
  );

  const [isGenerating, setIsGenerating] = useState(false);
  const [generatedPaper, setGeneratedPaper] = useState<ExamPaper | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadSubjects();
  }, []);

  useEffect(() => {
    if (selectedSubjectId) {
      loadBlueprints(Number(selectedSubjectId));
    }
  }, [selectedSubjectId]);

  const loadSubjects = async () => {
    try {
      const res = await apiClient.get<Subject[]>('/subjects');
      setSubjects(res.data);
      if (res.data.length > 0) {
        setSelectedSubjectId(res.data[0].id);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const loadBlueprints = async (subId: number) => {
    try {
      const res = await apiClient.get<ExamBlueprint[]>(`/teacher/blueprints?subjectId=${subId}`);
      setBlueprints(res.data);
      if (urlBlueprintId) {
        setSelectedBlueprintId(Number(urlBlueprintId));
      } else if (res.data.length > 0) {
        setSelectedBlueprintId(res.data[0].id);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedBlueprintId) {
      setError('Please select an exam blueprint to proceed.');
      return;
    }

    setError(null);
    setIsGenerating(true);
    setGeneratedPaper(null);

    try {
      const res = await apiClient.post<ExamPaper>('/teacher/generate', {
        blueprintId: Number(selectedBlueprintId),
        additionalInstructions: instructions,
      });

      setGeneratedPaper(res.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'AI generation failed. Please check your blueprints or try again.');
    } finally {
      setIsGenerating(false);
    }
  };

  const activeBlueprint = blueprints.find((b) => b.id === Number(selectedBlueprintId));

  if (isLoading) {
    return <LoadingSpinner message="Loading AI question synthesis console..." />;
  }

  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-slate-200">
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
          <Brain className="w-6 h-6 text-indigo-600" />
          AI Question Generator & Grounding Studio
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Synthesize high-fidelity question papers strictly grounded in uploaded subject documents, calibrated to blueprint difficulty.
        </p>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-sm flex items-start gap-2.5">
          <AlertCircle className="w-5 h-5 shrink-0 text-rose-600 mt-0.5" />
          <div>
            <p className="font-bold">Question Generation Error</p>
            <p className="text-xs mt-0.5">{error}</p>
          </div>
        </div>
      )}

      {/* Generation Complete Card */}
      {generatedPaper && (
        <div className="p-6 rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-600 text-white shadow-lg animate-in fade-in zoom-in-95">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="p-3 bg-white/20 rounded-xl">
                <CheckCircle2 className="w-8 h-8 text-white" />
              </div>
              <div>
                <h3 className="text-xl font-extrabold">{generatedPaper.title} Generated!</h3>
                <p className="text-xs text-white/90 mt-0.5">
                  Generated {generatedPaper.totalQuestions} questions ({generatedPaper.maxMarks} marks). Saved as DRAFT for your mandatory faculty review.
                </p>
              </div>
            </div>

            <button
              onClick={() => navigate(`/teacher/papers/${generatedPaper.id}`)}
              className="px-5 py-2.5 bg-white text-emerald-800 font-bold rounded-xl text-sm shadow-md hover:bg-emerald-50 transition flex items-center gap-2 shrink-0"
            >
              <span>Review & Approve Paper</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Generation Form */}
        <div className="lg:col-span-2 space-y-6">
          <form onSubmit={handleGenerate} className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs space-y-5">
            <div>
              <label className="block text-xs font-bold uppercase text-slate-600 mb-1">Subject</label>
              <select
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
              <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
                Select Exam Blueprint
              </label>
              {blueprints.length === 0 ? (
                <div className="p-3 bg-amber-50 text-amber-800 rounded-lg text-xs">
                  No blueprints found for this subject. Please define one in the "Exam Blueprints" tab.
                </div>
              ) : (
                <select
                  required
                  value={selectedBlueprintId}
                  onChange={(e) => setSelectedBlueprintId(Number(e.target.value))}
                  className="w-full px-3 py-2 border rounded-lg text-sm bg-white font-semibold"
                >
                  {blueprints.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.title} ({b.totalQuestions} Questions • E:{b.easyPercentage}% M:{b.mediumPercentage}% H:{b.hardPercentage}%)
                    </option>
                  ))}
                </select>
              )}
            </div>

            <div>
              <label className="block text-xs font-bold uppercase text-slate-600 mb-1">
                Teacher Natural-Language Guidance Prompt
              </label>
              <textarea
                rows={4}
                value={instructions}
                onChange={(e) => setInstructions(e.target.value)}
                placeholder="e.g. Generate 20 DBMS questions from SQL and Transactions. Make 30% easy, 50% medium, and 20% hard. Focus on the selected lesson material. Generate multiple-choice questions with four options and one correct answer. Include explanations and source references."
                className="w-full px-3 py-2.5 border rounded-lg text-sm font-sans"
              />
              <p className="text-[11px] text-slate-400 mt-1">
                The AI model references exclusively the lesson PDF/TXT notes of the chosen blueprint.
              </p>
            </div>

            <button
              type="submit"
              disabled={isGenerating || !selectedBlueprintId}
              className="w-full py-3 px-4 bg-gradient-to-r from-indigo-600 to-blue-600 hover:from-indigo-700 hover:to-blue-700 text-white rounded-xl text-sm font-bold shadow-md shadow-indigo-500/20 transition flex items-center justify-center gap-2 disabled:opacity-50"
            >
              {isGenerating ? (
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  <span>Synthesizing Grounded Questions via AI...</span>
                </div>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  <span>Generate Examination Paper Draft</span>
                </>
              )}
            </button>
          </form>
        </div>

        {/* Right Col: Active Blueprint Inspection */}
        <div className="space-y-4">
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
            <h3 className="text-xs font-bold uppercase text-slate-500 mb-3 tracking-wider">
              Selected Blueprint Specifications
            </h3>

            {activeBlueprint ? (
              <div className="space-y-3 text-xs">
                <div>
                  <span className="text-slate-400 block">Blueprint Title</span>
                  <span className="font-bold text-slate-900 text-sm">{activeBlueprint.title}</span>
                </div>
                <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-100">
                  <div>
                    <span className="text-slate-400">Total Questions</span>
                    <span className="font-bold text-slate-800 block">{activeBlueprint.totalQuestions}</span>
                  </div>
                  <div>
                    <span className="text-slate-400">Max Marks</span>
                    <span className="font-bold text-slate-800 block">{activeBlueprint.maxMarks}</span>
                  </div>
                </div>

                <div className="pt-2 border-t border-slate-100">
                  <span className="text-slate-400 block mb-1">Target Difficulty Distribution</span>
                  <div className="flex items-center gap-1.5 text-[11px] font-bold">
                    <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 border border-emerald-200">
                      Easy: {activeBlueprint.easyPercentage}% ({activeBlueprint.easyQuestionCount} Qs)
                    </span>
                    <span className="px-2 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-200">
                      Med: {activeBlueprint.mediumPercentage}% ({activeBlueprint.mediumQuestionCount} Qs)
                    </span>
                    <span className="px-2 py-0.5 rounded bg-purple-50 text-purple-700 border border-purple-200">
                      Hard: {activeBlueprint.hardPercentage}% ({activeBlueprint.hardQuestionCount} Qs)
                    </span>
                  </div>
                </div>

                <div className="pt-2 border-t border-slate-100">
                  <span className="text-slate-400 block mb-1">Target Curriculum Lessons</span>
                  <ul className="space-y-1">
                    {activeBlueprint.lessonBreakdowns.map((b) => (
                      <li key={b.lessonId} className="flex justify-between text-slate-700 font-medium">
                        <span className="truncate pr-2">• {b.lessonTitle}</span>
                        <span className="font-bold shrink-0">{b.targetPercentage}% ({b.questionCount} Qs)</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            ) : (
              <p className="text-xs text-slate-400">Select a blueprint to inspect parameters.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
