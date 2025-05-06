package dev.dfeprado.brokeragenote.core.exceptions;

public class ProtectedBrokerageNoteError extends BrokerageNoteReadError {

  private static final long serialVersionUID = 1L;

  public ProtectedBrokerageNoteError(String msg) {
    super(msg);
  }

}
