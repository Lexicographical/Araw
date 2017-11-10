package com.tamaraw.araw;

import android.app.Activity;
import android.support.v4.content.ContextCompat;

/**
 * Created by User on 8/26/2017.
 */

public enum DataType {

    Temperature(R.color.temperature, "(\u00b0C)", -40, 80),
    Humidity(R.color.humidity, "", 0, 100),
    HeatIndex(R.color.heatindex, "(\u00b0C)", -40, 80),
    Pressure(R.color.pressure, "(KPa)", 0, 110),
    CarbonMonoxide(R.color.carbonmonoxide, "ppm", 0, 10);

    String name;
    String unit;
    int color;
    int min;
    int max;

    DataType(int color, String unit, int min, int max) {
        this.name = name();
        if (name.equals("HeatIndex")) {
            this.name = "Heat Index";
        } else if (name.equals("CarbonMonoxide")) {
            this.name = "Carbon Monoxide";
        }
        this.color = color;
        this.unit = unit;
        this.min = min;
        this.max = max;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public int getColor(Activity activity) {
        return ContextCompat.getColor(activity.getApplicationContext(), color);
    }


}
