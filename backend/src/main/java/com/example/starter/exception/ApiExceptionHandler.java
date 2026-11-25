package com.example.starter.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException exception) {
        return response(exception.error());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) throws Exception {
        if (exception instanceof AuthenticationException || exception instanceof AccessDeniedException) {
            throw exception;
        }
        LOG.atError()
                .addKeyValue("errorType", exception.getClass().getName())
                .log("http.unexpected_error");
        return response(ApiError.INTERNAL_ERROR);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(status).headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ApiError.frameworkProblem(status));
    }

    private static ResponseEntity<ProblemDetail> response(ApiError error) {
        var body = error.problem();
        return ResponseEntity.status(body.getStatus()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
}
