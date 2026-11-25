package com.example.starter.exception;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

public enum ApiError {
    INVALID_SAMPLE(HttpStatus.BAD_REQUEST, "样本编号须为 1 至 120 个字符。"),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "角色只能设为查看者或操作员。"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "请求参数或正文格式有误。"),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "请使用有效账号登录。"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "当前账号没有此权限。"),
    DETECTION_TRANSPORT_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "算法服务暂时不可用，请稍后重试。"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "服务处理失败，请稍后重试。");

    private final HttpStatus status;
    private final String detail;

    ApiError(HttpStatus status, String detail) {
        this.status = status;
        this.detail = detail;
    }

    public ProblemDetail problem() {
        return problem(status, name(), detail);
    }

    static ProblemDetail frameworkProblem(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> INVALID_REQUEST.problem();
            case 401 -> AUTHENTICATION_REQUIRED.problem();
            case 403 -> ACCESS_DENIED.problem();
            case 500 -> INTERNAL_ERROR.problem();
            default -> problem(status, "HTTP_" + status.value(),
                    status.is5xxServerError() ? INTERNAL_ERROR.detail : "请求无法处理。");
        };
    }

    private static ProblemDetail problem(HttpStatusCode status, String code, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        String requestId = MDC.get("requestId");
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }
        return problem;
    }
}
