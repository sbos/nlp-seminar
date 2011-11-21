package ru.ispras.nlpcourse;

import com.google.code.javax.mail.*;
import com.google.code.javax.mail.search.FlagTerm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;

public class SubmissionProvider {
  private Store store;
  private Log log = LogFactory.getLog(SubmissionProvider.class);

  public SubmissionProvider() {
    initialize();
  }

  private void initialize() {
    log.debug("Forced to initialize");

    Properties properties = System.getProperties();
    properties.setProperty("mail.store.protocol", "imaps");
    properties.put("mail.imap.fetchsize", "819200");

    try {
      Session session = Session.getDefaultInstance(properties);
      store = session.getStore("imaps");
      store.connect("imap.gmail.com", Settings.getGoogleLogin(), Settings.getGooglePassword());
      log.debug("IMAPs store and session were obtained");
    } catch (NoSuchProviderException e) {
      log.error("Could not obtain IMAPs store", e);
    } catch (MessagingException e) {
      log.error("Could not connect to GMail", e);
    }
  }

  private void checkStoreConnected() {
    if (store == null)
      initialize();
    if (!store.isConnected())
      try {
        store.connect();
      } catch (MessagingException e) {
        log.error("Could not connect to IMAP store", e);
        initialize();
        checkStoreConnected();
      }
  }

  private Message[] fetchNewSubmissions() {
    checkStoreConnected();
    FragileAction<Message[]> hoho = new FragileAction<Message[]>() {
      @Override
      protected Message[] act() throws MessagingException {
        Folder submissions = store.getFolder("submissions");
        submissions.open(Folder.READ_ONLY);
        FlagTerm flagTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
        return submissions.search(flagTerm);
      }

      @Override
      protected void pickUpPieces() {
        checkStoreConnected();
      }
    };
    return hoho.go();
  }

  private Result fillResult(InputStream stream, Result result) {
    Scanner scanner = new Scanner(stream);
    result.precision = scanner.nextDouble();
    result.recall = scanner.nextDouble();
    result.time = scanner.nextDouble();
    return result;
  }

  private synchronized Submission<Result> process(Message message) throws  SubmissionProcessingException {
    Submission<Result> submission = new Submission<Result>();
    submission.result = new Result();

    try {
      if (message.getContent() instanceof Multipart) {
        Multipart multipartContent = (Multipart)message.getContent();
        for (int i = 0; i < multipartContent.getCount(); ++i) {
          BodyPart part = multipartContent.getBodyPart(i);
          if (Message.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
            final String filePath = Settings.getTemporaryPath() + "/" + part.getFileName();
            final File tmpFile = new File(filePath);
            //if (!tmpFile.createNewFile())
            //  throw new SubmissionProcessingException("Could not download attachment");
            FileOutputStream stream = new FileOutputStream(tmpFile);
            InputStream inputStream = part.getInputStream();
            byte[] buffer = new byte[1024 * 100];
            int length;
            while ((length = inputStream.read(buffer)) >= 0)
              stream.write(buffer, 0, length);
            stream.close();
            final String tmpDir = Settings.getTemporaryPath() + "/" + UUID.randomUUID().toString();
            Runtime runtime = Runtime.getRuntime();

            try {
              final File tmpDirFile = new File(tmpDir);
              if (!tmpDirFile.mkdirs())
                throw new SubmissionProcessingException("Could not create temporary directory for submission files");
              runtime.exec("unzip " + filePath + " -d " + tmpDir).waitFor();
            }
            catch (InterruptedException e) {
              throw new SubmissionProcessingException("An error occurred while unpacking the archive " + part.getFileName(),
                e);
            }
            finally {
              if (!tmpFile.delete())
                log.error("Couldn't delete temporary file " + tmpFile);
              //new File(filePath).;
            }

            try {
              runtime.exec("cp " + Settings.getTesterPath() + " " + tmpDir).waitFor();
            } catch (InterruptedException e) {
              throw new SubmissionProcessingException("Couldn't copy tester file to the tmp directory");
            }

            Process process = runtime.exec("timeout -s 9 " + Settings.getTimeoutMinutes() +
              "m python " + new File(Settings.getTesterPath()).getName(), new String[0],
              new File(tmpDir));
            try {
              process.waitFor();
            } catch (InterruptedException e) {
              log.error("An error occurred while running the tester", e);
            }
            submission.result.status = process.exitValue();

            if (submission.result.status == 0) {
              submission.result = fillResult(process.getInputStream(), submission.result);
            }
          } else {
            //if (part.getContentType().equalsIgnoreCase("text/plain")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(part.getInputStream()));
            submission.system = reader.readLine();
            submission.author = reader.readLine();
            submission.id = message.getMessageNumber();
            reader.close();
          //}
          }
        }
        message.setFlag(Flags.Flag.SEEN, true);
      } else {
        return null;
      }
    } catch (IOException e) {
      throw new SubmissionProcessingException(e);
    } catch (MessagingException e) {
      throw new SubmissionProcessingException(e);
    }

    return submission;
  }

  public Submission<Result>[] processNewSubmissions() {
    Message[] messages = fetchNewSubmissions();
    List<Submission> submissions = new ArrayList<Submission>(messages.length);
    for (Message message: messages) {
      try {
        Submission<Result> submission = process(message);
        if (submission != null)
          submissions.add(submission);
        }
      catch (SubmissionProcessingException e) {
        log.error("An error occurred while processing the submission", e);
      }
    }
    return (Submission<Result>[]) submissions.toArray();
  }
}
