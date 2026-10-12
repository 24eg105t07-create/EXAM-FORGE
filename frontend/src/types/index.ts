// TypeScript type definitions for ExamForge

export type Role = 'MANAGEMENT' | 'TEACHER' | 'STUDENT';
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
export type ProcessingStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
export type QuestionType = 'MCQ' | 'TRUE_FALSE' | 'SHORT_ANSWER';
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';
export type PaperStatus = 'DRAFT' | 'UNDER_REVIEW' | 'APPROVED' | 'PUBLISHED' | 'ARCHIVED';
export type ScheduleStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type ResultReleasePolicy = 'IMMEDIATE' | 'AFTER_EXAM_WINDOW' | 'MANUAL';
export type AttemptStatus = 'IN_PROGRESS' | 'SUBMITTED' | 'EVALUATED' | 'TIMED_OUT' | 'CANCELLED';
export type NotificationType = 'INFO' | 'WARNING' | 'SUCCESS' | 'ALERT';

export interface UserSummary {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  status: UserStatus;
  phone?: string;
  avatarUrl?: string;
  createdAt?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: UserSummary;
}

export interface Department {
  id: number;
  code: string;
  name: string;
  description?: string;
}

export interface AcademicYear {
  id: number;
  yearName: string;
  startDate: string;
  endDate: string;
  isCurrent: boolean;
}

export interface AcademicClass {
  id: number;
  departmentId: number;
  departmentName?: string;
  code: string;
  name: string;
  semester: number;
}

export interface Section {
  id: number;
  classId: number;
  className?: string;
  name: string;
}

export interface Subject {
  id: number;
  departmentId: number;
  departmentName?: string;
  code: string;
  name: string;
  description?: string;
  credits: number;
}

export interface StudentEnrollment {
  id: number;
  studentId: number;
  studentName: string;
  studentEmail: string;
  sectionId: number;
  sectionName: string;
  classId: number;
  className: string;
  rollNumber: string;
  academicYearId: number;
  academicYearName: string;
  status: string;
}

export interface TeacherAssignment {
  id: number;
  teacherId: number;
  teacherName: string;
  teacherEmail: string;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  classId: number;
  className: string;
  sectionId?: number;
  sectionName?: string;
}

export interface Lesson {
  id: number;
  subjectId: number;
  subjectName?: string;
  unitNumber: number;
  title: string;
  description?: string;
  orderIndex: number;
  documentCount?: number;
}

export interface DocumentItem {
  id: number;
  lessonId: number;
  lessonTitle: string;
  subjectId: number;
  subjectName: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  processingStatus: ProcessingStatus;
  extractedSnippet?: string;
  errorMessage?: string;
  createdAt: string;
}

export interface BlueprintLessonBreakdown {
  lessonId: number;
  lessonTitle: string;
  targetPercentage: number;
  questionCount: number;
}

export interface ExamBlueprint {
  id: number;
  subjectId: number;
  subjectName: string;
  title: string;
  totalQuestions: number;
  durationMinutes: number;
  maxMarks: number;
  easyPercentage: number;
  mediumPercentage: number;
  hardPercentage: number;
  easyQuestionCount: number;
  mediumQuestionCount: number;
  hardQuestionCount: number;
  mcqCount: number;
  trueFalseCount: number;
  shortAnswerCount: number;
  marksPerQuestion: number;
  instructions?: string;
  status: string;
  lessonBreakdowns: BlueprintLessonBreakdown[];
  createdAt: string;
}

export interface OptionItem {
  id?: number;
  optionLabel: string;
  optionText: string;
  isCorrect?: boolean;
  orderIndex?: number;
}

export interface QuestionItem {
  id: number;
  subjectId: number;
  lessonId: number;
  lessonTitle?: string;
  questionText: string;
  questionType: QuestionType;
  difficulty: Difficulty;
  marks: number;
  explanation?: string;
  sourceReference?: string;
  isAiGenerated: boolean;
  options: OptionItem[];
}

export interface PaperQuestion {
  id: number;
  questionOrder: number;
  marks: number;
  question: QuestionItem;
}

export interface ExamPaper {
  id: number;
  blueprintId?: number;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  creatorId: number;
  creatorName: string;
  title: string;
  totalQuestions: number;
  maxMarks: number;
  durationMinutes: number;
  status: PaperStatus;
  version: number;
  isLocked: boolean;
  approvedAt?: string;
  approvedByName?: string;
  questions: PaperQuestion[];
  createdAt: string;
}

export interface ExamSchedule {
  id: number;
  examPaperId: number;
  examPaperTitle: string;
  subjectId: number;
  subjectName: string;
  classId: number;
  className: string;
  sectionId?: number;
  sectionName?: string;
  title: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  maxMarks: number;
  instructions?: string;
  resultReleasePolicy: ResultReleasePolicy;
  isResultsReleased: boolean;
  status: ScheduleStatus;
  cancelledReason?: string;
  eligibleStudentsCount: number;
  attemptsCount: number;
  createdAt: string;
}

export interface StudentOption {
  id: number;
  optionLabel: string;
  optionText: string;
}

export interface StudentQuestion {
  questionId: number;
  questionOrder: number;
  questionText: string;
  questionType: QuestionType;
  marks: number;
  options: StudentOption[];
}

export interface SavedAnswer {
  questionId: number;
  selectedOptionId?: number;
  textAnswer?: string;
  isMarkedForReview: boolean;
  lastSavedAt?: string;
}

export interface StudentExamSession {
  attemptId: number;
  scheduleId: number;
  examTitle: string;
  subjectName: string;
  durationMinutes: number;
  maxMarks: number;
  instructions?: string;
  startTime: string;
  deadlineTime: string;
  remainingSeconds: number;
  status: AttemptStatus;
  questions: StudentQuestion[];
  savedAnswers: Record<number, SavedAnswer>;
}

export interface StudentAttemptSummary {
  attemptId: number;
  scheduleId: number;
  examTitle: string;
  subjectName: string;
  startTime: string;
  endTime?: string;
  status: string;
  totalScore?: number;
  maxMarks: number;
  percentage?: number;
  passed?: boolean;
  isResultReleased: boolean;
}

export interface BreakdownScore {
  totalQuestions: number;
  correctCount: number;
  totalMarks: number;
  marksObtained: number;
  percentage: number;
}

export interface QuestionResultDetail {
  questionId: number;
  questionOrder: number;
  questionText: string;
  questionType: QuestionType;
  difficulty: Difficulty;
  lessonTitle: string;
  selectedOptionId?: number;
  selectedOptionLabel?: string;
  selectedOptionText?: string;
  correctOptionLabel?: string;
  correctOptionText?: string;
  studentTextAnswer?: string;
  isCorrect?: boolean;
  marksAwarded: number;
  maxMarks: number;
  explanation?: string;
  teacherComment?: string;
}

export interface DetailedResult {
  attemptId: number;
  scheduleId: number;
  examTitle: string;
  subjectName: string;
  studentName: string;
  rollNumber: string;
  totalScore: number;
  maxMarks: number;
  percentage: number;
  passed: boolean;
  totalQuestions: number;
  correctCount: number;
  incorrectCount: number;
  unansweredCount: number;
  topicBreakdown: Record<string, BreakdownScore>;
  difficultyBreakdown: Record<string, BreakdownScore>;
  answers: QuestionResultDetail[];
  evaluatedAt: string;
}

export interface StudentRank {
  rank: number;
  studentId: number;
  studentName: string;
  rollNumber: string;
  score: number;
  percentage: number;
  passed: boolean;
}

export interface ClassAnalytics {
  scheduleId: number;
  examTitle: string;
  subjectName: string;
  className: string;
  totalEnrolled: number;
  attemptsCount: number;
  passedCount: number;
  passPercentage: number;
  averageScore: number;
  highestScore: number;
  lowestScore: number;
  maxMarks: number;
  rankings: StudentRank[];
}

export interface NotificationItem {
  id: number;
  title: string;
  message: string;
  type: NotificationType;
  isRead: boolean;
  link?: string;
  createdAt: string;
}

export interface AuditLogItem {
  id: number;
  userId?: number;
  userEmail: string;
  userName: string;
  action: string;
  entityType: string;
  entityId?: number;
  details?: string;
  ipAddress?: string;
  createdAt: string;
}
