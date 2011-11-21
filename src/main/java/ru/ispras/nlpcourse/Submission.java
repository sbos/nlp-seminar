package ru.ispras.nlpcourse;

public class Submission<TResult> {
  public String system, author;
  public int id;
  public TResult result;

  public Submission() {
  }

  public Submission(String system, String author, int id) {
    this.system = system;
    this.author = author;
    this.id = id;
  }
}
