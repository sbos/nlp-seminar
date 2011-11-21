package ru.ispras.nlpcourse;

public class SubmissionProcessingException extends Exception {
  public SubmissionProcessingException(String message, Throwable e) {
    super(message, e);
  }

  public SubmissionProcessingException(Throwable e) {
    super(e);
  }

  public SubmissionProcessingException() {
    super();
  }

  public SubmissionProcessingException(String message) {
    super(message);
  }
}
