package dev.dfeprado.tool;

import java.io.File;
import java.net.URISyntaxException;

public class ResourcesUtil {
  public File getSinacorBrokerageNoteResourceFile() throws URISyntaxException {
    return new File(getClass().getResource("/sinacor_brokerage_note.pdf").toURI());
  }
}
