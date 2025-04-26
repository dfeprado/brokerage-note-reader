package dev.dfeprado.brokeragenote.cmdrunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public abstract class AbstractMapTemplate<T> {
  private static String getWorkspaceDir() throws IOException {
    Path workspaceDir = Path.of(System.getProperty("user.home"), ".dfebrworkspace");
    if (!Files.exists(workspaceDir)) {
      Files.createDirectories(workspaceDir);
    } else {
      if (!Files.isDirectory(workspaceDir)) {
        throw new IOException("The workspace " + workspaceDir + " exists but is not a directory.");
      }
    }

    return workspaceDir.toString();
  }

  private String workspaceDir;
  private File mapFile;

  public AbstractMapTemplate(String name, int version) throws IOException {
    workspaceDir = getWorkspaceDir();
    mapFile = new File(Path.of(workspaceDir, name + version + ".map").toString());
    if (!mapFile.exists()) {
      createMap();
    } else {
      try (var oin = new ObjectInputStream(new FileInputStream(mapFile))) {
        loadMap(oin);
      } catch (ClassNotFoundException e) {
        // This will never happen, cause Map<String, String> is a default class of Java.
      }
    }
  }

  protected abstract void createMap();

  protected abstract void loadMap(ObjectInputStream oin) throws IOException, ClassNotFoundException;

  public abstract boolean has(String key);

  public abstract void set(String key, T value);

  public abstract T get(String key);

  protected void save(Object map) throws IOException {
    try (var oout = new ObjectOutputStream(new FileOutputStream(mapFile))) {
      oout.writeObject(map);
    }
  }

  public abstract Map<String, T> getMap();
}
