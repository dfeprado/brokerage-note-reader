package dev.dfeprado.brokeragenote.cmdrunner;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BrokersMap extends AbstractMapTemplate<String> {
  private static int version = 1;

  private OutputFormat outCtx;
  private Map<OutputFormat, Map<String, String>> map;

  public BrokersMap(OutputFormat outContext) throws IOException {
    super("brokers", version);
    this.outCtx = outContext;
  }

  @Override
  protected void createMap() {
    // TODO Auto-generated method stub
    map = new HashMap<>();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void loadMap(ObjectInputStream oin) throws IOException, ClassNotFoundException {
    map = (Map<OutputFormat, Map<String, String>>) oin.readObject();
  }

  @Override
  public boolean has(String brokerName) {
    var ctx = map.get(outCtx);
    return ctx != null && ctx.containsKey(brokerName);
  }

  @Override
  public void set(String brokerName, String brokerAlias) {
    getContext().put(brokerName, brokerAlias);
  }

  @Override
  public String get(String brokerName) {
    return getContext().getOrDefault(brokerName, "");
  }

  @Override
  public Map<String, String> getMap() {
    return Collections.unmodifiableMap(getContext());
  }

  private Map<String, String> getContext() {
    var ctx = map.get(outCtx);
    if (ctx == null) {
      ctx = new HashMap<String, String>();
      map.put(outCtx, ctx);
    }

    return ctx;
  }

  public void save() throws IOException {
    super.save(map);
  }
}
