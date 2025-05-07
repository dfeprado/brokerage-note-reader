package dev.dfeprado.brokeragenote.core.output;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public interface XlsxWriter {
  void writeToXslx(FileOutputStream output) throws FileNotFoundException, IOException;
}
