package me.ialistannen.tntspawnevents.libs;

public enum ExternalLibrary {
  LIBATTACH("/libattach", "libattach");

  private String basePath;
  private String executableName;

  ExternalLibrary(String basePath, String executableName) {
    this.basePath = basePath;
    this.executableName = executableName;
  }

  public String getBasePath() {
    return basePath;
  }

  public String getExecutableName() {
    return executableName;
  }
}
