package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.domain.exception.EmailAlreadyExistsException;
import com.carlos.mindsync.domain.exception.InvalidCredentialsException;
import com.carlos.mindsync.domain.exception.InvalidJournalEntryException;
import com.carlos.mindsync.domain.exception.InvalidUserException;
import com.carlos.mindsync.domain.exception.JournalEntryNotFoundException;
import com.carlos.mindsync.domain.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

// The parent class already turns Spring MVC errors (validation, malformed JSON, unknown path) into ProblemDetail.
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler({InvalidUserException.class, InvalidJournalEntryException.class})
	ProblemDetail handleInvalidData(RuntimeException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler(EmailAlreadyExistsException.class)
	ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ProblemDetail handleInvalidCredentials(InvalidCredentialsException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
	}

	@ExceptionHandler({UserNotFoundException.class, JournalEntryNotFoundException.class})
	ProblemDetail handleNotFound(RuntimeException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	// One entry per failing field. The rejected value is never included: it could be a password or a private note.
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "request validation failed");
		List<Map<String, String>> errors = e.getBindingResult().getAllErrors().stream()
				.map(error -> Map.of(
						"field", error instanceof FieldError field ? field.getField() : error.getObjectName(),
						"message", String.valueOf(error.getDefaultMessage())))
				.sorted(Comparator.comparing((Map<String, String> m) -> m.get("field"))
						.thenComparing(m -> m.get("message")))
				.toList();
		problem.setProperty("errors", errors);
		return handleExceptionInternal(e, problem, headers, status, request);
	}

	// The default message repeats the offending value; say only which parameter was wrong.
	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException e, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String name = e.getPropertyName() == null ? "parameter" : e.getPropertyName();
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"invalid value for '" + name + "'");
		return handleExceptionInternal(e, problem, headers, status, request);
	}

	// Log the details, but never send them to the client.
	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception e) {
		log.error("Unexpected error", e);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "unexpected error");
	}
}
