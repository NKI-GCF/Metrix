package nki.exceptions;

import java.lang.Exception;

public class InvalidCredentialsException extends Exception {

  public InvalidCredentialsException(String message) {
    super(message);
  }

  public InvalidCredentialsException(String message, Throwable throwable) {
    super(message, throwable);
  }

}
