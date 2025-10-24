package io.github.nahomgh.portfolio.exceptions;

public class AssetNotFoundException extends RuntimeException{
    public AssetNotFoundException() {
    }

    public AssetNotFoundException(String message) {
        super(message);
    }
}
