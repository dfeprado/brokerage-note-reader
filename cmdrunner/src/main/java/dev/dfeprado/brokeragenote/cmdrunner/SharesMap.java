package dev.dfeprado.brokeragenote.cmdrunner;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import dev.dfeprado.brokeragenote.core.output.statusinvest.ShareSymbol;

public class SharesMap extends AbstractMapTemplate<ShareSymbol> {
  private static int version = 1;

  private Map<String, ShareSymbol> map;

  public SharesMap() throws IOException {
    super("shares", version);
  }

  @Override
  protected void createMap() {
    // TODO Auto-generated method stub
    map = new HashMap<>();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void loadMap(ObjectInputStream oin) throws IOException, ClassNotFoundException {
    map = (Map<String, ShareSymbol>) oin.readObject();
  }

  @Override
  public boolean has(String key) {
    return map.containsKey(key);
  }

  @Override
  public void set(String key, ShareSymbol value) {
    map.put(key, value);
  }

  @Override
  public ShareSymbol get(String key) {
    return map.get(key);
  }

  @Override
  public Map<String, ShareSymbol> getMap() {
    return Collections.unmodifiableMap(map);
  }

  protected void save() throws IOException {
    super.save(map);
  }
}
