package sophie517.testtask.kkd.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import sophie517.testtask.kkd.api.dto.KkdResponses.ErrorDetail;
import sophie517.testtask.kkd.api.dto.KkdResponses.ErrorResponse;
import sophie517.testtask.kkd.application.service.RunNotFoundException;
import sophie517.testtask.kkd.application.service.UnitNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RunNotFoundException.class)
    public ResponseEntity<Object> runNotFound(RunNotFoundException error) {
        return response(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", error.getMessage());
    }

    @ExceptionHandler(UnitNotFoundException.class)
    public ResponseEntity<Object> unitNotFound(UnitNotFoundException error) {
        return response(HttpStatus.NOT_FOUND, "UNIT_NOT_FOUND", error.getMessage());
    }

    @ExceptionHandler(InvalidParamsException.class)
    public ResponseEntity<Object> invalidParams(InvalidParamsException error) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PARAMS", error.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> internalError(Exception error) {
        LOG.error("Request processing failed", error);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error");
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception error, Object body, HttpHeaders headers,
                                                              HttpStatusCode status, WebRequest request) {
        int code = status.value() == 400 ? 422 : status.value();
        String errorCode = switch (code) {
            case 422 -> "INVALID_PARAMS";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            default -> "HTTP_ERROR";
        };
        return new ResponseEntity<>(new ErrorResponse(new ErrorDetail(errorCode,
                code == 422 ? "Invalid request parameters" : "Request could not be processed")), headers,
                HttpStatusCode.valueOf(code));
    }

    private ResponseEntity<Object> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(new ErrorDetail(code, message)));
    }
}
