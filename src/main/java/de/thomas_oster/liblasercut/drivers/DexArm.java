/*
  This file is part of LibLaserCut.
  Copyright (C) 2011 - 2014 Thomas Oster <mail@thomas-oster.de>

  LibLaserCut is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  LibLaserCut is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with LibLaserCut. If not, see <http://www.gnu.org/licenses/>.

 */

package de.thomas_oster.liblasercut.drivers;

import de.thomas_oster.liblasercut.IllegalJobException;
import de.thomas_oster.liblasercut.JobPart;
import de.thomas_oster.liblasercut.LaserCutter;
import de.thomas_oster.liblasercut.LaserJob;
import de.thomas_oster.liblasercut.LaserProperty;
import de.thomas_oster.liblasercut.PowerSpeedFocusFrequencyProperty;
import de.thomas_oster.liblasercut.ProgressListener;
import de.thomas_oster.liblasercut.drivers.GenericGcodeDriver;
import de.thomas_oster.liblasercut.VectorCommand;
import de.thomas_oster.liblasercut.VectorPart;
import de.thomas_oster.liblasercut.platform.Util;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.smartcardio.ATR;

public class DexArm extends Marlin {
  protected static final String SETTING_PEN_DROP_DISTANCE = "Pen drop distance (in mm)";
  protected static final String SETTING_LIFT_PEN_AFTER_EVERY_LINE = "Lift pen after every line";

  /**
   * This is the height the pen gets dropped down from WCS Z0.
   */
  protected Double penDropDistance = 10.0;

  /**
   * If this is true, the pen will be lifted after every single line operation and
   * not only during moving operations. This might beusefull when there are a lot
   * of sharp angles in the lines that would lead to a bending of the pen in the
   * corners.
   */
  protected Boolean liftPenAfterEveryLine = false;

  public DexArm() {
    // remove homing and M5 (which shouldn't be available on Marlin anyway?!)
    setPreJobGcode(getPreJobGcode().replace(",G28 XY,M5", ""));
    // add returning to Z0 after the job and remove M5 and homing at the end.
    setPostJobGcode("G0 Z0," + getPostJobGcode().replace(",M5,G28 XY", ""));
  }

  public Double getPenDropDistance() {
    return penDropDistance;
  }

  public void setPenDropDistance(Double penDropDistance) {
    this.penDropDistance = penDropDistance;
  }

  public Boolean getLiftPenAfterEveryLine() {
    return liftPenAfterEveryLine;
  }

  public void setLiftPenAfterEveryLine(Boolean liftPenAfterEveryLine) {
    this.liftPenAfterEveryLine = liftPenAfterEveryLine;
  }

  @Override
  protected void move(PrintStream out, double x, double y, double resolution) throws IOException {
    x = isFlipXaxis() ? getBedWidth() - Util.px2mm(x, resolution) : Util.px2mm(x, resolution);
    y = isFlipYaxis() ? getBedHeight() - Util.px2mm(y, resolution) : Util.px2mm(y, resolution);
    currentSpeed = getTravel_speed();

    // lift pen
    sendLine("G0 Z0");

    if (blankLaserDuringRapids) {
      currentPower = 0.0;
      sendLine("G0 X%f Y%f F%d S0", x, y, (int) (travel_speed));
    } else {
      sendLine("G0 X%f Y%f F%d", x, y, (int) (travel_speed));
    }
  }

  @Override
  protected void line(PrintStream out, double x, double y, double resolution) throws IOException {
    x = isFlipXaxis() ? getBedWidth() - Util.px2mm(x, resolution) : Util.px2mm(x, resolution);
    y = isFlipYaxis() ? getBedHeight() - Util.px2mm(y, resolution) : Util.px2mm(y, resolution);
    String append = "";
    if (nextPower != currentPower) {
      append += String.format(FORMAT_LOCALE, " S%f", nextPower);
      currentPower = nextPower;
    }
    if (nextSpeed != currentSpeed) {
      append += String.format(FORMAT_LOCALE, " F%d", (int) (max_speed * nextSpeed / 100.0));
      currentSpeed = nextSpeed;
    }

    // drop pen
    sendLine("G0 Z-%f", this.penDropDistance);
    sendLine("G1 X%f Y%f" + append, x, y);

    if (this.liftPenAfterEveryLine) {
      // lift pen
      sendLine("G0 Z0");
    }
  }

  @Override
  public String getModelName() {
    return "DexArm Driver";
  }

  @Override
  public DexArm clone() {
    DexArm clone = new DexArm();
    clone.copyProperties(this);
    return clone;
  }

  @Override
  public String[] getPropertyKeys() {
    List<String> result = new LinkedList<>(Arrays.asList(super.getPropertyKeys()));

    // remove unused properties from Marlin
    result.remove(GenericGcodeDriver.SETTING_FLIP_X);
    result.remove(GenericGcodeDriver.SETTING_FLIP_Y);

    // add DexArm specific properties
    result.add(DexArm.SETTING_PEN_DROP_DISTANCE);
    result.add(DexArm.SETTING_LIFT_PEN_AFTER_EVERY_LINE);

    return result.toArray(new String[0]);
  }

  @Override
  public Object getProperty(String attribute) {
    Object result = super.getProperty(attribute);

    if (result == null) {
      if (SETTING_PEN_DROP_DISTANCE.equals(attribute)) {
        result = this.getPenDropDistance();
      } else if (SETTING_LIFT_PEN_AFTER_EVERY_LINE.equals(attribute)) {
        result = this.getLiftPenAfterEveryLine();
      }
    }

    return result;
  }

  @Override
  public void setProperty(String attribute, Object value) {
    super.setProperty(attribute, value);

    if (SETTING_PEN_DROP_DISTANCE.equals(attribute)) {
      this.setPenDropDistance((Double) value);
    } else if (SETTING_LIFT_PEN_AFTER_EVERY_LINE.equals(attribute)) {
      this.setLiftPenAfterEveryLine((Boolean) value);
    }
  }
}
