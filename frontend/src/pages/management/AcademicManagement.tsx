import React, { useState, useEffect } from 'react';
import { apiClient } from '../../api/client';
import {
  Department,
  AcademicClass,
  Section,
  Subject,
  TeacherAssignment,
  StudentEnrollment,
  UserSummary,
  AcademicYear,
} from '../../types';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Modal } from '../../components/common/Modal';
import { Plus, GraduationCap, BookOpen, Users, Building2 } from 'lucide-react';

export const AcademicManagement: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'departments' | 'classes' | 'subjects' | 'assignments' | 'enrollments'>('departments');
  const [departments, setDepartments] = useState<Department[]>([]);
  const [classes, setClasses] = useState<AcademicClass[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [assignments, setAssignments] = useState<TeacherAssignment[]>([]);
  const [enrollments, setEnrollments] = useState<StudentEnrollment[]>([]);
  const [teachers, setTeachers] = useState<UserSummary[]>([]);
  const [students, setStudents] = useState<UserSummary[]>([]);
  const [academicYears, setAcademicYears] = useState<AcademicYear[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Modals state
  const [modalType, setModalType] = useState<string | null>(null);

  // Form states
  const [deptCode, setDeptCode] = useState('');
  const [deptName, setDeptName] = useState('');
  const [deptDesc, setDeptDesc] = useState('');

  const [classDeptId, setClassDeptId] = useState<number | ''>('');
  const [classCode, setClassCode] = useState('');
  const [className, setClassName] = useState('');
  const [classSemester, setClassSemester] = useState<number>(5);

  const [subjectDeptId, setSubjectDeptId] = useState<number | ''>('');
  const [subjectCode, setSubjectCode] = useState('');
  const [subjectName, setSubjectName] = useState('');
  const [subjectCredits, setSubjectCredits] = useState<number>(4);

  const [assignTeacherId, setAssignTeacherId] = useState<number | ''>('');
  const [assignSubjectId, setAssignSubjectId] = useState<number | ''>('');
  const [assignClassId, setAssignClassId] = useState<number | ''>('');

  const [enrollStudentId, setEnrollStudentId] = useState<number | ''>('');
  const [enrollSectionId, setEnrollSectionId] = useState<number | ''>('');
  const [enrollRollNo, setEnrollRollNo] = useState('');

  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    loadAllData();
  }, []);

  const loadAllData = async () => {
    setIsLoading(true);
    try {
      const [deptRes, classRes, subRes, assignRes, yearRes, teachRes, studRes] = await Promise.all([
        apiClient.get<Department[]>('/academic/departments'),
        apiClient.get<AcademicClass[]>('/academic/classes'),
        apiClient.get<Subject[]>('/subjects'),
        apiClient.get<TeacherAssignment[]>('/academic/assignments'),
        apiClient.get<AcademicYear[]>('/academic/years'),
        apiClient.get<{ content: UserSummary[] }>('/management/users?role=TEACHER&size=100'),
        apiClient.get<{ content: UserSummary[] }>('/management/users?role=STUDENT&size=100'),
      ]);

      setDepartments(deptRes.data);
      setClasses(classRes.data);
      setSubjects(subRes.data);
      setAssignments(assignRes.data);
      setAcademicYears(yearRes.data);
      setTeachers(teachRes.data.content || []);
      setStudents(studRes.data.content || []);

      if (classRes.data.length > 0) {
        const enrollRes = await apiClient.get<StudentEnrollment[]>(`/academic/classes/${classRes.data[0].id}/enrollments`);
        setEnrollments(enrollRes.data);
      }
    } catch (err) {
      console.error('Failed to load academic data', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateDept = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    try {
      await apiClient.post('/management/departments', { code: deptCode, name: deptName, description: deptDesc });
      setModalType(null);
      setDeptCode(''); setDeptName(''); setDeptDesc('');
      loadAllData();
    } catch (err: any) {
      setFormError(err.response?.data?.message || 'Failed to create department');
    }
  };

  const handleCreateClass = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    try {
      await apiClient.post('/management/classes', {
        departmentId: Number(classDeptId),
        code: classCode,
        name: className,
        semester: Number(classSemester),
      });
      setModalType(null);
      setClassCode(''); setClassName('');
      loadAllData();
    } catch (err: any) {
      setFormError(err.response?.data?.message || 'Failed to create class');
    }
  };

  const handleCreateSubject = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    try {
      await apiClient.post('/subjects', {
        departmentId: Number(subjectDeptId),
        code: subjectCode,
        name: subjectName,
        credits: Number(subjectCredits),
      });
      setModalType(null);
      setSubjectCode(''); setSubjectName('');
      loadAllData();
    } catch (err: any) {
      setFormError(err.response?.data?.message || 'Failed to create subject');
    }
  };

  const handleAssignTeacher = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    try {
      const currentYear = academicYears.find((y) => y.isCurrent) || academicYears[0];
      await apiClient.post('/management/assignments', {
        teacherId: Number(assignTeacherId),
        subjectId: Number(assignSubjectId),
        classId: Number(assignClassId),
        sectionId: null,
        academicYearId: currentYear.id,
      });
      setModalType(null);
      loadAllData();
    } catch (err: any) {
      setFormError(err.response?.data?.message || 'Failed to assign teacher');
    }
  };

  const handleEnrollStudent = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    try {
      const currentYear = academicYears.find((y) => y.isCurrent) || academicYears[0];
      await apiClient.post('/management/enrollments', {
        studentId: Number(enrollStudentId),
        sectionId: Number(enrollSectionId),
        rollNumber: enrollRollNo,
        academicYearId: currentYear.id,
      });
      setModalType(null);
      setEnrollRollNo('');
      loadAllData();
    } catch (err: any) {
      setFormError(err.response?.data?.message || 'Failed to enroll student');
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Loading academic infrastructure..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Academic Architecture</h1>
          <p className="text-sm text-slate-500 mt-1">
            Configure college departments, classes, courses, teacher allocations, and student rosters.
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-200 gap-6 text-sm font-semibold">
        {[
          { key: 'departments', label: 'Departments', icon: Building2 },
          { key: 'classes', label: 'Classes / Batches', icon: GraduationCap },
          { key: 'subjects', label: 'Subjects (Courses)', icon: BookOpen },
          { key: 'assignments', label: 'Teacher Allocations', icon: Users },
          { key: 'enrollments', label: 'Student Enrollments', icon: Users },
        ].map((tab) => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key as any)}
              className={`pb-3 flex items-center gap-2 border-b-2 transition ${
                activeTab === tab.key
                  ? 'border-blue-600 text-blue-600'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              <Icon className="w-4 h-4" />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Departments Tab */}
      {activeTab === 'departments' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="text-base font-bold text-slate-900">Registered Departments</h3>
            <button
              onClick={() => setModalType('dept')}
              className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5"
            >
              <Plus className="w-3.5 h-3.5" /> Add Department
            </button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {departments.map((d) => (
              <div key={d.id} className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
                <div className="flex justify-between items-start">
                  <h4 className="font-bold text-slate-900">{d.name}</h4>
                  <span className="text-xs font-bold px-2 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-200">
                    {d.code}
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-2 line-clamp-2">{d.description || 'No description provided'}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Classes Tab */}
      {activeTab === 'classes' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="text-base font-bold text-slate-900">Academic Classes & Programs</h3>
            <button
              onClick={() => setModalType('class')}
              className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5"
            >
              <Plus className="w-3.5 h-3.5" /> Add Class
            </button>
          </div>
          <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase">
                <tr>
                  <th className="py-3 px-4">Class Code</th>
                  <th className="py-3 px-4">Class Name</th>
                  <th className="py-3 px-4">Department</th>
                  <th className="py-3 px-4">Semester</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {classes.map((c) => (
                  <tr key={c.id}>
                    <td className="py-3 px-4 font-bold text-slate-800">{c.code}</td>
                    <td className="py-3 px-4 font-medium text-slate-900">{c.name}</td>
                    <td className="py-3 px-4 text-xs text-slate-600">{c.departmentName}</td>
                    <td className="py-3 px-4 text-xs font-semibold text-slate-700">Sem {c.semester}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Subjects Tab */}
      {activeTab === 'subjects' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="text-base font-bold text-slate-900">Courses & Subjects</h3>
            <button
              onClick={() => setModalType('subject')}
              className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5"
            >
              <Plus className="w-3.5 h-3.5" /> Add Subject
            </button>
          </div>
          <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase">
                <tr>
                  <th className="py-3 px-4">Course Code</th>
                  <th className="py-3 px-4">Subject Title</th>
                  <th className="py-3 px-4">Department</th>
                  <th className="py-3 px-4">Credits</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {subjects.map((s) => (
                  <tr key={s.id}>
                    <td className="py-3 px-4 font-bold text-blue-700">{s.code}</td>
                    <td className="py-3 px-4 font-medium text-slate-900">{s.name}</td>
                    <td className="py-3 px-4 text-xs text-slate-600">{s.departmentName}</td>
                    <td className="py-3 px-4 text-xs font-semibold text-slate-700">{s.credits} Credits</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Teacher Assignments Tab */}
      {activeTab === 'assignments' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="text-base font-bold text-slate-900">Teacher Course Allocations</h3>
            <button
              onClick={() => setModalType('assign')}
              className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5"
            >
              <Plus className="w-3.5 h-3.5" /> Allocate Subject to Faculty
            </button>
          </div>
          <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase">
                <tr>
                  <th className="py-3 px-4">Faculty Member</th>
                  <th className="py-3 px-4">Subject</th>
                  <th className="py-3 px-4">Class</th>
                  <th className="py-3 px-4">Section</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {assignments.map((a) => (
                  <tr key={a.id}>
                    <td className="py-3 px-4">
                      <div className="font-semibold text-slate-900">{a.teacherName}</div>
                      <div className="text-xs text-slate-500">{a.teacherEmail}</div>
                    </td>
                    <td className="py-3 px-4">
                      <span className="font-bold text-blue-700 mr-1.5">[{a.subjectCode}]</span>
                      <span className="text-slate-800 font-medium">{a.subjectName}</span>
                    </td>
                    <td className="py-3 px-4 text-xs font-medium text-slate-700">{a.className}</td>
                    <td className="py-3 px-4 text-xs font-semibold text-slate-600">{a.sectionName || 'All Sections'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Student Enrollments Tab */}
      {activeTab === 'enrollments' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="text-base font-bold text-slate-900">Student Class Enrollments</h3>
            <button
              onClick={() => setModalType('enroll')}
              className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5"
            >
              <Plus className="w-3.5 h-3.5" /> Enroll Student
            </button>
          </div>
          <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-600 uppercase">
                <tr>
                  <th className="py-3 px-4">Roll Number</th>
                  <th className="py-3 px-4">Student</th>
                  <th className="py-3 px-4">Class / Section</th>
                  <th className="py-3 px-4">Academic Year</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {enrollments.map((e) => (
                  <tr key={e.id}>
                    <td className="py-3 px-4 font-mono font-bold text-blue-700">{e.rollNumber}</td>
                    <td className="py-3 px-4">
                      <div className="font-semibold text-slate-900">{e.studentName}</div>
                      <div className="text-xs text-slate-500">{e.studentEmail}</div>
                    </td>
                    <td className="py-3 px-4 text-xs font-medium text-slate-700">
                      {e.className} — {e.sectionName}
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-600">{e.academicYearName}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal: Department */}
      <Modal isOpen={modalType === 'dept'} onClose={() => setModalType(null)} title="Add Department">
        {formError && <div className="mb-3 text-xs text-rose-700 bg-rose-50 p-2.5 rounded">{formError}</div>}
        <form onSubmit={handleCreateDept} className="space-y-3">
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Code</label>
            <input required value={deptCode} onChange={(e) => setDeptCode(e.target.value)} placeholder="e.g. MECH" className="w-full px-3 py-2 border rounded-lg text-sm" />
          </div>
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Name</label>
            <input required value={deptName} onChange={(e) => setDeptName(e.target.value)} placeholder="Mechanical Engineering" className="w-full px-3 py-2 border rounded-lg text-sm" />
          </div>
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Description</label>
            <textarea value={deptDesc} onChange={(e) => setDeptDesc(e.target.value)} className="w-full px-3 py-2 border rounded-lg text-sm" rows={2} />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-semibold">Save Department</button>
          </div>
        </form>
      </Modal>

      {/* Modal: Class */}
      <Modal isOpen={modalType === 'class'} onClose={() => setModalType(null)} title="Add Class Batch">
        {formError && <div className="mb-3 text-xs text-rose-700 bg-rose-50 p-2.5 rounded">{formError}</div>}
        <form onSubmit={handleCreateClass} className="space-y-3">
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Department</label>
            <select required value={classDeptId} onChange={(e) => setClassDeptId(Number(e.target.value))} className="w-full px-3 py-2 border rounded-lg text-sm bg-white">
              <option value="">Select Department</option>
              {departments.map((d) => <option key={d.id} value={d.id}>{d.name} ({d.code})</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Class Code</label>
            <input required value={classCode} onChange={(e) => setClassCode(e.target.value)} placeholder="CS-401" className="w-full px-3 py-2 border rounded-lg text-sm" />
          </div>
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Class Name</label>
            <input required value={className} onChange={(e) => setClassName(e.target.value)} placeholder="B.Tech Computer Science 4th Year" className="w-full px-3 py-2 border rounded-lg text-sm" />
          </div>
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Semester</label>
            <input type="number" required value={classSemester} onChange={(e) => setClassSemester(Number(e.target.value))} className="w-full px-3 py-2 border rounded-lg text-sm" />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-semibold">Save Class</button>
          </div>
        </form>
      </Modal>

      {/* Modal: Subject */}
      <Modal isOpen={modalType === 'subject'} onClose={() => setModalType(null)} title="Add Course / Subject">
        {formError && <div className="mb-3 text-xs text-rose-700 bg-rose-50 p-2.5 rounded">{formError}</div>}
        <form onSubmit={handleCreateSubject} className="space-y-3">
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Department</label>
            <select required value={subjectDeptId} onChange={(e) => setSubjectDeptId(Number(e.target.value))} className="w-full px-3 py-2 border rounded-lg text-sm bg-white">
              <option value="">Select Department</option>
              {departments.map((d) => <option key={d.id} value={d.id}>{d.name} ({d.code})</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Course Code</label>
            <input required value={subjectCode} onChange={(e) => setSubjectCode(e.target.value)} placeholder="CS304" className="w-full px-3 py-2 border rounded-lg text-sm" />
          </div>
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Subject Name</label>
            <input required value={subjectName} onChange={(e) => setSubjectName(e.target.value)} placeholder="Artificial Intelligence" className="w-full px-3 py-2 border rounded-lg text-sm" />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-semibold">Save Subject</button>
          </div>
        </form>
      </Modal>

      {/* Modal: Teacher Assignment */}
      <Modal isOpen={modalType === 'assign'} onClose={() => setModalType(null)} title="Allocate Course to Faculty">
        {formError && <div className="mb-3 text-xs text-rose-700 bg-rose-50 p-2.5 rounded">{formError}</div>}
        <form onSubmit={handleAssignTeacher} className="space-y-3">
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Faculty Member</label>
            <select required value={assignTeacherId} onChange={(e) => setAssignTeacherId(Number(e.target.value))} className="w-full px-3 py-2 border rounded-lg text-sm bg-white">
              <option value="">Select Teacher</option>
              {teachers.map((t) => <option key={t.id} value={t.id}>{t.fullName} ({t.email})</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Subject</label>
            <select required value={assignSubjectId} onChange={(e) => setAssignSubjectId(Number(e.target.value))} className="w-full px-3 py-2 border rounded-lg text-sm bg-white">
              <option value="">Select Subject</option>
              {subjects.map((s) => <option key={s.id} value={s.id}>[{s.code}] {s.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Class</label>
            <select required value={assignClassId} onChange={(e) => setAssignClassId(Number(e.target.value))} className="w-full px-3 py-2 border rounded-lg text-sm bg-white">
              <option value="">Select Class</option>
              {classes.map((c) => <option key={c.id} value={c.id}>{c.name} ({c.code})</option>)}
            </select>
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-semibold">Confirm Assignment</button>
          </div>
        </form>
      </Modal>

      {/* Modal: Student Enrollment */}
      <Modal isOpen={modalType === 'enroll'} onClose={() => setModalType(null)} title="Enroll Student into Section">
        {formError && <div className="mb-3 text-xs text-rose-700 bg-rose-50 p-2.5 rounded">{formError}</div>}
        <form onSubmit={handleEnrollStudent} className="space-y-3">
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Student</label>
            <select required value={enrollStudentId} onChange={(e) => setEnrollStudentId(Number(e.target.value))} className="w-full px-3 py-2 border rounded-lg text-sm bg-white">
              <option value="">Select Student</option>
              {students.map((s) => <option key={s.id} value={s.id}>{s.fullName} ({s.email})</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Section</label>
            <select required value={enrollSectionId} onChange={(e) => setEnrollSectionId(Number(e.target.value))} className="w-full px-3 py-2 border rounded-lg text-sm bg-white">
              <option value="">Select Section</option>
              <option value={1}>B.Tech CSE 3rd Year — Section A</option>
              <option value={2}>B.Tech CSE 3rd Year — Section B</option>
            </select>
          </div>
          <div>
            <label className="block text-xs font-bold uppercase mb-1">Roll Number</label>
            <input required value={enrollRollNo} onChange={(e) => setEnrollRollNo(e.target.value)} placeholder="e.g. 2026CSE003" className="w-full px-3 py-2 border rounded-lg text-sm" />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-semibold">Confirm Enrollment</button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
