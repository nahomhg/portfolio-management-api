package io.github.nahomgh.portfolio.exceptions;

public class PriceUnavailableException extends RuntimeException{
    public PriceUnavailableException(){

    }
    public PriceUnavailableException(String message){
        super(message);
    }
}
