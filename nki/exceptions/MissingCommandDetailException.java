package nki.exceptions;

import java.lang.Exception;

public class MissingCommandDetailException extends Exception {

    public MissingCommandDetailException(String message) {
        super(message);
    }

    public MissingCommandDetailException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
