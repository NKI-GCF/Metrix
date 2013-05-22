package nki.exceptions;

import java.lang.Exception;

public class EmptyResultSetCollection extends Exception {

    public EmptyResultSetCollection(String message) {
        super(message);
    }

    public EmptyResultSetCollection(String message, Throwable throwable) {
        super(message, throwable);
    }

}
