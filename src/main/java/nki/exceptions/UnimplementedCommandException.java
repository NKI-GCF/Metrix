package nki.exceptions;

import java.lang.Exception;

public class UnimplementedCommandException extends Exception {

    public UnimplementedCommandException(String message) {
        super(message);
    }

    public UnimplementedCommandException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
