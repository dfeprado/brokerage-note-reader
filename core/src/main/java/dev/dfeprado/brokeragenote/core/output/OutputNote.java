package dev.dfeprado.brokeragenote.core.output;

import java.io.File;
import java.io.IOException;

public interface OutputNote {
  void summarizeCreatedFile(File file) throws IOException;
}
