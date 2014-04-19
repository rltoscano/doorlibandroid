package net.gombi.door.lib;

/** Immutable Door object. */
public class Door {
  private String key;
  private String displayName;
  private String regId;
  private String devId;

  @SuppressWarnings("unused")  // Used by Gson.
  private Door() {}

  public Door(String key, String displayName, String regId, String devId) {
    this.key = key;
    this.displayName = displayName;
    this.regId = regId;
    this.devId = devId;
  }

  public String getKey() { return key; }
  public String getDisplayName() { return displayName; }
  public String getRegId() { return regId; }
  public Door setRegId(String regId) { return new Door(key, displayName, regId, devId); }
  public String getDevId() { return devId; }
  @Override public String toString() { return displayName; }
}
