package dev.dfeprado.brokeragenote.core.exceptions;

public class BrokerageNoteReadError extends Exception {
  private static final long serialVersionUID = 1L;

  public BrokerageNoteReadError(String msg) {
    super(msg);
  }
}
