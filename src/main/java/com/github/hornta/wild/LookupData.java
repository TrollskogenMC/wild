package com.github.hornta.wild;

import com.wimbli.WorldBorder.BorderData;

public class LookupData {
  private boolean isRound;
  private BorderData wbBorderData;
  private int centerX;
  private int centerZ;
  private int radiusX;
  private int radiusZ;

  public LookupData(boolean isRound, BorderData wbBorderData, int centerX, int centerZ, int radiusX, int radiusZ) {
    this.isRound = isRound;
    this.wbBorderData = wbBorderData;
    this.centerX = centerX;
    this.centerZ = centerZ;
    this.radiusX = radiusX;
    this.radiusZ = radiusZ;
  }

  public boolean isRound() {
    return isRound;
  }

  public BorderData getWbBorderData() {
    return wbBorderData;
  }

  public int getCenterX() {
    return centerX;
  }

  public int getCenterZ() {
    return centerZ;
  }

  public int getRadiusZ() {
    return radiusZ;
  }

  public int getRadiusX() {
    return radiusX;
  }
}
