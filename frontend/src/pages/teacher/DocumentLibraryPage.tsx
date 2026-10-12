import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { apiClient } from '../../api/client';
import { Subject, Lesson, DocumentItem } from '../../types';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { StatusBadge } from '../../components/common/StatusBadge';
import { Modal } from '../../components/common/Modal';
import { Upload, Plus, Trash2, FileText, CheckCircle2, AlertTriangle, Search, Eye } from 'lucide-react';

export const DocumentLibraryPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const initialSubjectId = searchParams.get('subjectId');

  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | null>(
    initialSubjectId ? Number(initialSubjectId) : null
  );
  const [lessons, setLessons] = useState<Lesson[]>([]);
  const [selectedLessonId, setSelectedLessonId] = useState<number | null>(null);
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Modals & previews
  const [isUnitModalOpen, setIsUnitModalOpen] = useState(false);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [previewDoc, setPreviewDoc] = useState<DocumentItem | null>(null);

  // New Lesson State
  const [unitNumber, setUnitNumber] = useState<number>(1);
  const [unitTitle, setUnitTitle] = useState('');
  const [unitDesc, setUnitDesc] = useState('');

  // Upload State
  const [uploadLessonId, setUploadLessonId] = useState<number | ''>('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  useEffect(() => {
    loadSubjects();
  }, []);

  useEffect(() => {
    if (selectedSubjectId) {
      loadLessonsAndDocs(selectedSubjectId);
    }
  }, [selectedSubjectId]);

  const loadSubjects = async () => {
    try {
      const res = await apiClient.get<Subject[]>('/subjects');
      setSubjects(res.data);
      if (res.data.length > 0 && !selectedSubjectId) {
        setSelectedSubjectId(res.data[0].id);
      }
    } catch (err) {
      console.error('Failed to load subjects', err);
    } finally {
      setIsLoading(false);
    }
  };

  const loadLessonsAndDocs = async (subId: number) => {
    setIsLoading(true);
    try {
      const [lessonRes, docRes] = await Promise.all([
        apiClient.get<Lesson[]>(`/teacher/lessons?subjectId=${subId}`),
        apiClient.get<DocumentItem[]>(`/teacher/documents?subjectId=${subId}`),
      ]);
      setLessons(lessonRes.data);
      setDocuments(docRes.data);
    } catch (err) {
      console.error('Failed to load lessons/docs', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateLesson = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSubjectId) return;

    try {
      await apiClient.post('/teacher/lessons', {
        subjectId: selectedSubjectId,
        unitNumber,
        title: unitTitle,
        description: unitDesc,
      });
      setIsUnitModalOpen(false);
      setUnitTitle('');
      setUnitDesc('');
      loadLessonsAndDocs(selectedSubjectId);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create lesson unit');
    }
  };

  const handleUploadDocument = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadLessonId || !selectedFile) {
      setUploadError('Please select both a target lesson and a file.');
      return;
    }

    setUploadError(null);
    setIsUploading(true);

    const formData = new FormData();
    formData.append('file', selectedFile);

    try {
      await apiClient.post(`/teacher/lessons/${uploadLessonId}/documents`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setIsUploadModalOpen(false);
      setSelectedFile(null);
      if (selectedSubjectId) loadLessonsAndDocs(selectedSubjectId);
    } catch (err: any) {
      setUploadError(err.response?.data?.message || 'Upload failed');
    } finally {
      setIsUploading(false);
    }
  };

  const handleDeleteDoc = async (id: number) => {
    if (!confirm('Are you sure you want to delete this document?')) return;
    try {
      await apiClient.delete(`/teacher/documents/${id}`);
      if (selectedSubjectId) loadLessonsAndDocs(selectedSubjectId);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to delete document');
    }
  };

  const filteredDocs = selectedLessonId
    ? documents.filter((d) => d.lessonId === selectedLessonId)
    : documents;

  if (isLoading && subjects.length === 0) {
    return <LoadingSpinner message="Loading curriculum repository..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Lesson Document Repository</h1>
          <p className="text-sm text-slate-500 mt-1">
            Upload PDF/TXT lecture notes and syllabus units to ground AI question generation.
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => setIsUnitModalOpen(true)}
            className="px-3 py-2 bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-lg text-xs font-semibold shadow-xs transition flex items-center gap-1.5"
          >
            <Plus className="w-3.5 h-3.5" /> Add Unit / Lesson
          </button>
          <button
            onClick={() => {
              setUploadLessonId(lessons.length > 0 ? lessons[0].id : '');
              setIsUploadModalOpen(true);
            }}
            disabled={lessons.length === 0}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold shadow-sm transition flex items-center gap-1.5 disabled:opacity-50"
          >
            <Upload className="w-3.5 h-3.5" /> Upload Document
          </button>
        </div>
      </div>

      {/* Subject Selector Bar */}
      <div className="flex items-center gap-4 bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs">
        <span className="text-xs font-bold uppercase text-slate-500">Active Subject:</span>
        <select
          value={selectedSubjectId || ''}
          onChange={(e) => {
            const val = Number(e.target.value);
            setSelectedSubjectId(val);
            setSelectedLessonId(null);
          }}
          className="border border-slate-200 rounded-lg py-1.5 px-3 text-sm font-semibold text-slate-800 bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-600"
        >
          {subjects.map((s) => (
            <option key={s.id} value={s.id}>
              [{s.code}] {s.name}
            </option>
          ))}
        </select>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Lesson Units Sidebar */}
        <div className="lg:col-span-1 space-y-2">
          <h3 className="text-xs font-bold uppercase text-slate-500 px-1">Curriculum Units</h3>
          <button
            onClick={() => setSelectedLessonId(null)}
            className={`w-full text-left p-3 rounded-xl border text-xs font-semibold transition ${
              selectedLessonId === null
                ? 'bg-blue-600 text-white border-blue-600 shadow-xs'
                : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
            }`}
          >
            All Units ({documents.length} docs)
          </button>
          {lessons.map((l) => (
            <button
              key={l.id}
              onClick={() => setSelectedLessonId(l.id)}
              className={`w-full text-left p-3 rounded-xl border text-xs transition ${
                selectedLessonId === l.id
                  ? 'bg-blue-600 text-white border-blue-600 shadow-xs'
                  : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
              }`}
            >
              <div className="font-bold">Unit {l.unitNumber}: {l.title}</div>
              <div className={`text-[11px] mt-0.5 ${selectedLessonId === l.id ? 'text-blue-100' : 'text-slate-400'}`}>
                {documents.filter((d) => d.lessonId === l.id).length} documents
              </div>
            </button>
          ))}
        </div>

        {/* Documents List */}
        <div className="lg:col-span-3">
          <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
            {filteredDocs.length === 0 ? (
              <div className="p-12 text-center text-slate-500 text-sm">
                No documents uploaded for this selection yet. Click "Upload Document" to add PDFs or text notes.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase">
                    <tr>
                      <th className="py-3 px-4">Document</th>
                      <th className="py-3 px-4">Lesson Unit</th>
                      <th className="py-3 px-4">Status & Extraction</th>
                      <th className="py-3 px-4">Date</th>
                      <th className="py-3 px-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {filteredDocs.map((doc) => (
                      <tr key={doc.id} className="hover:bg-slate-50/50 transition">
                        <td className="py-3.5 px-4">
                          <div className="flex items-center gap-2">
                            <FileText className="w-4 h-4 text-blue-600 shrink-0" />
                            <div>
                              <div className="font-semibold text-slate-900">{doc.fileName}</div>
                              <div className="text-[11px] text-slate-400">
                                {(doc.fileSize / 1024).toFixed(1)} KB • {doc.mimeType}
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className="py-3.5 px-4 text-xs font-medium text-slate-700">
                          {doc.lessonTitle}
                        </td>
                        <td className="py-3.5 px-4">
                          <div className="flex flex-col items-start gap-1">
                            <StatusBadge status={doc.processingStatus} />
                            {doc.errorMessage && (
                              <span className="text-[11px] text-rose-600 font-medium" title={doc.errorMessage}>
                                {doc.errorMessage}
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="py-3.5 px-4 text-xs text-slate-500">
                          {new Date(doc.createdAt).toLocaleDateString()}
                        </td>
                        <td className="py-3.5 px-4 text-right space-x-1.5">
                          {doc.extractedSnippet && (
                            <button
                              onClick={() => setPreviewDoc(doc)}
                              className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition"
                              title="View extracted text snippet"
                            >
                              <Eye className="w-4 h-4" />
                            </button>
                          )}
                          <button
                            onClick={() => handleDeleteDoc(doc.id)}
                            className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
                            title="Delete document"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Modal: New Unit / Lesson */}
      <Modal isOpen={isUnitModalOpen} onClose={() => setIsUnitModalOpen(false)} title="Create Curriculum Unit">
        <form onSubmit={handleCreateLesson} className="space-y-4">
          <div>
            <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Unit Number</label>
            <input
              type="number"
              required
              min={1}
              value={unitNumber}
              onChange={(e) => setUnitNumber(Number(e.target.value))}
              className="w-full px-3 py-2 border rounded-lg text-sm"
            />
          </div>
          <div>
            <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Unit / Lesson Title</label>
            <input
              type="text"
              required
              value={unitTitle}
              onChange={(e) => setUnitTitle(e.target.value)}
              placeholder="e.g. Transactions & Concurrency Control"
              className="w-full px-3 py-2 border rounded-lg text-sm"
            />
          </div>
          <div>
            <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Description</label>
            <textarea
              value={unitDesc}
              onChange={(e) => setUnitDesc(e.target.value)}
              rows={2}
              placeholder="Key concepts, ACID, locks, and recovery"
              className="w-full px-3 py-2 border rounded-lg text-sm"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-semibold">
              Save Unit
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal: Upload Document */}
      <Modal isOpen={isUploadModalOpen} onClose={() => setIsUploadModalOpen(false)} title="Upload Lesson Document">
        {uploadError && (
          <div className="mb-4 p-3 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-lg">
            {uploadError}
          </div>
        )}
        <form onSubmit={handleUploadDocument} className="space-y-4">
          <div>
            <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Target Curriculum Unit</label>
            <select
              required
              value={uploadLessonId}
              onChange={(e) => setUploadLessonId(Number(e.target.value))}
              className="w-full px-3 py-2 border rounded-lg text-sm bg-white"
            >
              <option value="">Select Unit</option>
              {lessons.map((l) => (
                <option key={l.id} value={l.id}>
                  Unit {l.unitNumber}: {l.title}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase text-slate-700 mb-1">Select File (PDF or TXT)</label>
            <input
              type="file"
              required
              accept=".pdf,.txt,text/plain,application/pdf"
              onChange={(e) => setSelectedFile(e.target.files ? e.target.files[0] : null)}
              className="w-full text-sm text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
            />
            <p className="text-[11px] text-slate-400 mt-1">Maximum size 15 MB. Text extraction occurs automatically via PDFBox.</p>
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="submit"
              disabled={isUploading}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-semibold disabled:opacity-50"
            >
              {isUploading ? 'Uploading & Extracting...' : 'Upload & Process'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal: Extracted Text Preview */}
      <Modal isOpen={!!previewDoc} onClose={() => setPreviewDoc(null)} title={`Extracted Content: ${previewDoc?.fileName}`}>
        <div className="space-y-3">
          <div className="text-xs text-slate-500">
            <strong>Lesson:</strong> {previewDoc?.lessonTitle} • <strong>Status:</strong> {previewDoc?.processingStatus}
          </div>
          <div className="p-3 bg-slate-50 rounded-lg border border-slate-200 text-xs font-mono text-slate-800 max-h-96 overflow-y-auto whitespace-pre-wrap">
            {previewDoc?.extractedSnippet || 'No text extracted'}
          </div>
        </div>
      </Modal>
    </div>
  );
};
