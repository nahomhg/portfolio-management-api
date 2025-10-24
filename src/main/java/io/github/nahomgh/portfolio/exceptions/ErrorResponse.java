package io.github.nahomgh.portfolio.exceptions;

public class ErrorResponse {

    private String message;

    public ErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
