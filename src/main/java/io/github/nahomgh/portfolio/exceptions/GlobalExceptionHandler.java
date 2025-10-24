package io.github.nahomgh.portfolio.exceptions;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException resourceNotFound){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(resourceNotFound.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException userAlreadyExistsException){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(userAlreadyExistsException.getMessage()));
    }

    @ExceptionHandler(InputValidationException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleInvalidInputException(InputValidationException inputValidationException){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(inputValidationException.getMessage()));
    }

    @ExceptionHandler(TransactionFailedException.class)
    public ResponseEntity<ErrorResponse> handleUserTransactionFailedException(TransactionFailedException transactionFailedException){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(transactionFailedException.getMessage()));
    }

    @ExceptionHandler(AssetNotFoundException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleAssetNotFoundException(AssetNotFoundException assetNotFound){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(assetNotFound.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleInsufficientFundsException(InsufficientFundsException insufficientFundsException){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(insufficientFundsException.getMessage()));
    }

    @ExceptionHandler(PriceUnavailableException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handlePriceNotAvailableException(PriceUnavailableException priceUnavailableException){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(priceUnavailableException.getMessage()));
    }

    @ExceptionHandler(UnsupportedAssetException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleUnsupportedAssetException(UnsupportedAssetException unsupportedAssetException){
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse(unsupportedAssetException.getMessage()));
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTransactionException(DuplicateTransactionException duplicateTransactionException){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(duplicateTransactionException.getMessage()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(){
        return ResponseEntity.status(HttpStatus.CONFLICT.value()).
                body(new ErrorResponse("Concurrent Modification Failed\nUnable to complete transaction due to concurrent modification, please try again"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException constraintViolationException){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(constraintViolationException.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleMethodNotValidException(MethodArgumentNotValidException methodArgumentNotValidException){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(methodArgumentNotValidException.getMessage()));
    }



}