export interface UserResponse {
  id: number
  name: string
  email: string
}

export interface AuthResponse {
  token: string
  tokenType: string
  expiresInSeconds: number
  user: UserResponse
}

export interface StatusSummary {
  code: string
  name: string
}

/** The catalogue from GET /api/statuses. Never hardcode these client-side. */
export interface StatusResponse {
  id: number
  code: string
  name: string
  displayOrder: number
}

export interface ApplicationListItem {
  id: number
  companyName: string
  roleName: string
  currentStatus: StatusSummary
  portalUrl: string | null
  dateApplied: string
  resumeFilename: string | null
  updatedAt: string
}

export interface ProgressEvent {
  id: number
  statusId: number
  status: StatusSummary
  eventDate: string
  notes: string | null
}

export interface ApplicationDetail {
  id: number
  companyName: string
  roleName: string
  jobDescription: string | null
  portalUrl: string | null
  dateApplied: string
  currentStatus: StatusSummary
  resumeFilename: string | null
  notes: string | null
  createdAt: string
  updatedAt: string
  progress: ProgressEvent[]
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface StatusCount {
  statusCode: string
  statusName: string
  count: number
}

export interface DashboardResponse {
  totalApplications: number
  interviewCount: number
  offerCount: number
  statusBreakdown: StatusCount[]
  recentApplications: ApplicationListItem[]
}

/** Best-effort result of POST /job-imports. Any field may be null if extraction failed. */
export interface JobImportResponse {
  companyName: string | null
  roleName: string | null
  jobDescription: string | null
}

export interface ApplicationPayload {
  companyName: string
  roleName: string
  jobDescription?: string | null
  portalUrl?: string | null
  dateApplied: string
  notes?: string | null
}

export interface ProgressPayload {
  statusId: number
  eventDate: string
  notes?: string | null
}

/** The error envelope from MVP.md 6.3. */
export interface ApiErrorBody {
  timestamp: string
  status: number
  code: string
  message: string
  path: string
  fieldErrors: { field: string; message: string }[]
}
