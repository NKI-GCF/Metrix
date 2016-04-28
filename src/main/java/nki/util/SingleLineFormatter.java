package nki.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.text.SimpleDateFormat;

public final class SingleLineFormatter extends Formatter {
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");

  @Override
  public String format(LogRecord record) {
    Date dateObj = new Date(record.getMillis());
    // formatting Date with time information
    SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("E dd-MM-yyyy HH:mm:ss");
    String date = DATE_FORMAT.format(dateObj);

    StringBuilder sb = new StringBuilder();
    sb.append("[" + record.getLevel().getLocalizedName() + "] ").append("[" + date).append("] ").append("[" + record.getSourceClassName() + "]").append(" : ").append(formatMessage(record)).append(LINE_SEPARATOR);

    if (record.getThrown() != null) {
      try {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
          record.getThrown().printStackTrace(pw);
        }
        sb.append(sw.toString());
      }
      catch (Exception ex) {
        // ignore
      }
    }

    return sb.toString();
  }
}
