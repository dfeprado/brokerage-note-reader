package dev.dfeprado.brokeragenote.core;

import java.io.File;
import java.net.URISyntaxException;

public class ResourcesUtil {
  public static final String NOTE_SAMPLE_1 = "sinacor_brokerage_note.pdf";
  public static final String NOTE_SAMPLE_2 = "sinacor_brokerage_note_2.pdf";
  public static final String PROTECTED_NOTE_SAMPLE_1_123_PASSWORD =
      "123_password_protected_sinacor_brokerage_note.pdf";

  public File getSinacorBrokerageNoteResourceFile() throws URISyntaxException {
    return getSinacorBrokerageNoteResourceFile(NOTE_SAMPLE_1);
  }

  public File getSinacorBrokerageNoteResourceFile(String fileName) throws URISyntaxException {
    return new File(getClass().getResource("/" + fileName).toURI());
  }
}
