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

/**
 * This class should act as a starting-point, when implementing a new Lasercutter driver.
 * It will take a Laserjob and just output the Vecor-Parts as G-Code.
 * 
 * The file contains comments prefixed with "#<step>" which should guide you in the process
 * of creating custom drivers. Also read the information in the Wiki on
 * https://github.com/t-oster/VisiCut/wiki/
 * 
 * #1: Create a new JavaClass, which extends the de.thomas_oster.liblasercut.drivers.LaserCutter class
 * #2: Implement all abstract methods. Each of them is explained in this example.
 * #3: In Order to see your driver in VisiCut, add your class to the getSupportedDrivers() method
 * in the de.thomas_oster.liblasercut.LibInfo class (src/de/thomas_oster/liblasercut/LibInfo.java)
 * 
 * @author Thomas Oster
 */
public class DexArm extends Marlin
{

  public DexArm() {
    setPreJobGcode(getPreJobGcode().replace(",G28 XY,M5", ""));
    setPostJobGcode("G0 Z0," + getPostJobGcode().replace(",M5,G28 XY", ""));
  }

  @Override
  protected void move(PrintStream out, double x, double y, double resolution) throws IOException {
    x = isFlipXaxis() ? getBedWidth() - Util.px2mm(x, resolution) : Util.px2mm(x, resolution);
    y = isFlipYaxis() ? getBedHeight() - Util.px2mm(y, resolution) : Util.px2mm(y, resolution);
    currentSpeed = getTravel_speed();

    // lift pen
    sendLine("G0 Z0");

    if (blankLaserDuringRapids)
    {
      currentPower = 0.0;
      sendLine("G0 X%f Y%f F%d S0", x, y, (int) (travel_speed));
    }
    else
    {
      sendLine("G0 X%f Y%f F%d", x, y, (int) (travel_speed));
    }
  }

  @Override
  protected void line(PrintStream out, double x, double y, double resolution) throws IOException {
    x = isFlipXaxis() ? getBedWidth() - Util.px2mm(x, resolution) : Util.px2mm(x, resolution);
    y = isFlipYaxis() ? getBedHeight() - Util.px2mm(y, resolution) : Util.px2mm(y, resolution);
    String append = "";
    if (nextPower != currentPower)
    {
      append += String.format(FORMAT_LOCALE, " S%f", nextPower);
      currentPower = nextPower;
    }
    if (nextSpeed != currentSpeed)
    {
      append += String.format(FORMAT_LOCALE, " F%d", (int) (max_speed*nextSpeed/100.0));
      currentSpeed = nextSpeed;
    }

    // drop pen
    sendLine("G0 Z-10");
    sendLine("G1 X%f Y%f"+append, x, y);

    // lift pen
    sendLine("G0 Z0");
  }
  
  // /**
  //  * This method should return an Object of a class extending LaserProperty.
  //  * A LaserProperty represents all settings for your device like power,speed and frequency
  //  * which are necessary for a certain job-type (e.g. a VectorPart).
  //  * See the different classes for examples. We will just use the default,
  //  * supporting power,speed focus and frequency.
  //  */
  // @Override
  // public LaserProperty getLaserPropertyForVectorPart() {
  //     return new PowerSpeedFocusFrequencyProperty();
  // }
  

  // /**
  //  * This method should return a list of all supported resolutions (in DPI)
  //  */
  // @Override
  // public List<Double> getResolutions()
  // {
  //   return Arrays.asList(100.0,200.0,500.0,1000.0);
  // }

  // /**
  //  * This method should return the width of the laser-bed. You can have
  //  * a config-setting in order to have different sizes for each instance of 
  //  * your driver. For simplicity we just assume a width of 600mm
  //  */
  // @Override
  // public double getBedWidth()
  // {
  //   return 600;
  // }

  // /**
  //  * This method should return the height of the laser-bed. You can have
  //  * a config-setting in order to have different sizes for each instance of 
  //  * your driver. For simplicity we just assume a height of 300mm
  //  */
  // @Override
  // public double getBedHeight()
  // {
  //   return 300;
  // }

  /**
   * This method should return a name for this driver.
   */
  @Override
  public String getModelName()
  {
    return "DexArm Driver";
  }

  /**
   * This method must copy the current instance with all config settings, because
   * it is used for save- and restoring
   */
  @Override
  public DexArm clone()
  {
    DexArm clone = new DexArm();
    //TODO: copy all settings to the clone if present.
    clone.copyProperties(this);
    return clone;
  }

  // /**
  //  * The next mehtod allow for a generic GUI with settings for an instance of this
  //  * driver to be created. For simplicity, this driver does not support any
  //  * properties. Look at the other implementations for reference.
  //  */
  // @Override
  // public String[] getPropertyKeys()
  // {
  //   List<String> result = new LinkedList<>(Arrays.asList(super.getPropertyKeys()));
  //   result.remove(GenericGcodeDriver.SETTING_IDENTIFICATION_STRING);
  //   result.remove(GenericGcodeDriver.SETTING_WAIT_FOR_OK);
  //   result.remove(GenericGcodeDriver.SETTING_LINEEND);
  //   result.remove(GenericGcodeDriver.SETTING_INIT_DELAY);
  //   result.remove(GenericGcodeDriver.SETTING_HTTP_UPLOAD_URL);
  //   result.remove(GenericGcodeDriver.SETTING_HOST);
  //   result.remove(GenericGcodeDriver.SETTING_SPINDLE_MAX);
  //   result.remove(GenericGcodeDriver.SETTING_BLANK_LASER_DURING_RAPIDS);
  //   return result.toArray(new String[0]);
  // }

  // @Override
  // public void setProperty(String key, Object value)
  // {
  //   //should never be called
  //   throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  // }

  // @Override
  // public Object getProperty(String key)
  // {
  //   //should never be called
  //   throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  // }
  
}
