package dev.dfeprado.tool;

public abstract class Utils {
  public static double toNumber(String value) {
    return Double.parseDouble(value.replaceAll("\\.", "").replaceAll(",", "."));
  }
}
