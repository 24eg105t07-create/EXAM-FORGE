import React, { useState, useEffect } from 'react';
import { apiClient } from '../../api/client';
import { Subject, ExamPaper, ExamBlueprint } from '../../types';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { StatusBadge } from '../../components/common/StatusBadge';
import { Link } from 'react-router-dom';
import { BookOpen, FileText, Sparkles, ClipboardCheck, ArrowRight, FolderOpen } from 'lucide-react';

export const TeacherDashboard: React.FC = () => {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [recentPapers, setRecentPapers] = useState<ExamPaper[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadDashboard();
  }, []);

  const loadDashboard = async () => {
    setIsLoading(true);
    try {
      const subRes = await apiClient.get<Subject[]>('/subjects');
      setSubjects(subRes.data);

      if (subRes.data.length > 0) {
        const paperRes = await apiClient.get<ExamPaper[]>(`/teacher/papers?subjectId=${subRes.data[0].id}`);
        setRecentPapers(paperRes.data);
      }
    } catch (err) {
      console.error('Failed to load teacher dashboard', err);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading faculty course workspace..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Faculty Examination Workspace</h1>
          <p className="text-sm text-slate-500 mt-1">
            Curate curriculum documents, design question blueprints, generate AI papers, and review drafts.
          </p>
        </div>
        <div className="flex gap-2">
          <Link
            to="/teacher/blueprints"
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-semibold shadow-sm transition flex items-center gap-1.5"
          >
            <Sparkles className="w-4 h-4" /> New AI Blueprint
          </Link>
        </div>
      </div>

      {/* Course Subjects Grid */}
      <div>
        <h3 className="text-base font-bold text-slate-900 mb-3 flex items-center gap-2">
          <BookOpen className="w-5 h-5 text-blue-600" />
          My Assigned Courses & Subjects
        </h3>
        {subjects.length === 0 ? (
          <div className="bg-white p-6 rounded-xl border border-slate-200 text-center text-slate-500 text-sm">
            No subjects assigned yet. Please request College Management to assign your courses.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {subjects.map((sub) => (
              <div key={sub.id} className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs flex flex-col justify-between hover:border-blue-300 transition">
                <div>
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-mono font-bold px-2 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-200">
                      {sub.code}
                    </span>
                    <span className="text-xs font-semibold text-slate-500">{sub.credits} Credits</span>
                  </div>
                  <h4 className="font-bold text-slate-900 text-base mt-2">{sub.name}</h4>
                  <p className="text-xs text-slate-500 mt-1 line-clamp-2">{sub.description || 'Core departmental subject'}</p>
                </div>
                <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                  <Link
                    to={`/teacher/documents?subjectId=${sub.id}`}
                    className="text-blue-600 font-semibold hover:text-blue-800 flex items-center gap-1"
                  >
                    <FolderOpen className="w-3.5 h-3.5" /> Materials
                  </Link>
                  <Link
                    to={`/teacher/blueprints?subjectId=${sub.id}`}
                    className="text-purple-600 font-semibold hover:text-purple-800 flex items-center gap-1"
                  >
                    <FileText className="w-3.5 h-3.5" /> Blueprints
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Recent Question Papers */}
      <div className="mt-8">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
            <ClipboardCheck className="w-5 h-5 text-indigo-600" />
            Recent Question Papers & Drafts
          </h3>
          <Link to="/teacher/papers" className="text-xs font-semibold text-blue-600 hover:text-blue-800">
            View all papers →
          </Link>
        </div>

        <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
          {recentPapers.length === 0 ? (
            <div className="p-8 text-center text-slate-500 text-sm">
              No question papers generated yet. Start by uploading lesson documents and defining a blueprint.
            </div>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs font-bold text-slate-600 uppercase border-b border-slate-200">
                <tr>
                  <th className="py-3 px-4">Paper Title</th>
                  <th className="py-3 px-4">Subject</th>
                  <th className="py-3 px-4">Questions & Marks</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {recentPapers.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50/50 transition">
                    <td className="py-3 px-4">
                      <div className="font-semibold text-slate-900">{p.title}</div>
                      <div className="text-xs text-slate-400">Version {p.version}</div>
                    </td>
                    <td className="py-3 px-4 text-xs font-medium text-slate-700">
                      {p.subjectName}
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-600">
                      {p.totalQuestions} Questions • Max {p.maxMarks} Marks
                    </td>
                    <td className="py-3 px-4">
                      <StatusBadge status={p.status} />
                    </td>
                    <td className="py-3 px-4 text-right">
                      <Link
                        to={`/teacher/papers/${p.id}`}
                        className="px-3 py-1 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-lg text-xs font-semibold transition"
                      >
                        Review / Edit
                      </Link>
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
