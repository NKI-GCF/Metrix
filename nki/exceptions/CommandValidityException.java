package nki.exceptions;

import java.lang.Exception;

public class CommandValidityException extends Exception {

  public CommandValidityException(String message) {
    super(message);
  }

  public CommandValidityException(String message, Throwable throwable) {
    super(message, throwable);
  }

}
