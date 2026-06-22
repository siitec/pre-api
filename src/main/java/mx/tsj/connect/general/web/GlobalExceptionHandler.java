package mx.tsj.connect.general.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import mx.tsj.connect.general.web.support.ApiResponses;
import mx.tsj.connect.general.services.AuthenticationService.InvalidCredentialsException;
import mx.tsj.connect.general.services.PartidasAccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<?> handleInvalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(401)
                .body(ApiResponses.error("INVALID_CREDENTIALS", exception.getMessage()));
    }

    @ExceptionHandler(PartidasAccessDeniedException.class)
    public ResponseEntity<?> handlePartidasAccessDenied(PartidasAccessDeniedException exception) {
        return ResponseEntity.status(403)
                .body(ApiResponses.error("ACCESS_DENIED", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        log.warn("Type mismatch: {}", exception.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponses.error("BAD_REQUEST", "Los parametros enviados no son validos."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException exception) {
        log.warn("Validation error: {}", exception.getMessage());
        return ResponseEntity.status(422)
                .body(ApiResponses.error("VALIDATION_ERROR", "La solicitud contiene datos invalidos."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception exception) {
        log.error("Unhandled exception: ", exception);
        return ResponseEntity.status(500)
                .body(ApiResponses.error("INTERNAL_ERROR", "Ocurrio un error interno."));
    }
}
