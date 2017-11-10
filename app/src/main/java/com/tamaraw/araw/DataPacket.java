package com.tamaraw.araw;

/**
 * Created by User on 8/27/2017.
 */

public class DataPacket {

    private double temp;
    private double humidity;
    private double pressure;
    private double heatIndex;
    private boolean co;

    private final double R0 = 0;

    public DataPacket(double[] data) {
        this.temp = data[0];
        this.humidity = data[1];
        this.heatIndex = data[2];
        this.pressure = data[3] / 1000;
        this.co = data[4] == 0;
    }

    public double getTemp() {
        return temp;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getPressure() {
        return pressure;
    }

    public double getHeatIndex() {
        return heatIndex;
    }

    public boolean getCarbonMonoxide() {
        return co;
    }

}
