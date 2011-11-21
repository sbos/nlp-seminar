package ru.ispras.nlpcourse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SubmissionHandler {
  private Log log = LogFactory.getLog(SubmissionHandler.class);

  public void start() {
    SubmissionProvider provider = new SubmissionProvider();
    provider.processNewSubmissions();
  }

  public static void main(String[] args) {
    SubmissionHandler handler = new SubmissionHandler();
    handler.start();
  }
}
