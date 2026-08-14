package com.kejelah.pencarikeje.common;

/** Stable machine-readable error codes referenced by the frontend and by tests. */
public final class ErrorCodes {

    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
    public static final String INVALID_SORT_KEY = "INVALID_SORT_KEY";

    public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    public static final String INCORRECT_PASSWORD = "INCORRECT_PASSWORD";

    public static final String APPLICATION_NOT_FOUND = "APPLICATION_NOT_FOUND";
    public static final String APPLICATION_ACCESS_DENIED = "APPLICATION_ACCESS_DENIED";
    public static final String PROGRESS_NOT_FOUND = "PROGRESS_NOT_FOUND";
    public static final String LAST_PROGRESS_EVENT = "LAST_PROGRESS_EVENT";
    public static final String STATUS_NOT_FOUND = "STATUS_NOT_FOUND";

    public static final String INVALID_FILE_TYPE = "INVALID_FILE_TYPE";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String RESUME_NOT_FOUND = "RESUME_NOT_FOUND";
    public static final String STORAGE_FAILURE = "STORAGE_FAILURE";

    public static final String JOB_URL_UNSUPPORTED = "JOB_URL_UNSUPPORTED";
    public static final String JOB_URL_UNREACHABLE = "JOB_URL_UNREACHABLE";

    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private ErrorCodes() {
    }
}
