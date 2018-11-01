package com.tamaraw.araw;

import android.bluetooth.BluetoothAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.BufferedReader;
import java.util.UUID;

/**
 * Created by User on 11/9/2017.
 */

public class Constants {

    public static BufferedReader btReader;
    public static BluetoothAdapter adapter;
    public static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static int lastIndex = 0;
    public static String address;

    public static DataType chartType = DataType.Temperature;
    public static TextView dataLabel;

    public static LineChart chart;
    public static LineDataSet dataset;
    public static LineData data;
    public static int dataSize = 10;

    public static ImageButton[] chartButtons = new ImageButton[6];

    public static TextView coLabel;
    public static ImageView coStatusIcon;
    public static Button testAlarm;
    public static EditText[] inputs = new EditText[5];
    public static String[] contacts = new String[5];
    public static Button btnSavePhoneNum;
    public static Button btnSMSTest;
    public static LinearLayout settingsContainer;
    /**
     * Snooze state of carbon monoxide alarm
     * true - alarm will not go off (disabled in 30s)
     * false - alarm will go off
     */
    public static boolean coSnooze = false;
    public static boolean smsSnooze = false;

    /**
     * Status of carbon monoxide levels
     * true - danger levels
     * false - safe levels
     */
    public static boolean coStatus = false;

    public static boolean coDialogOpen = false;

    public static boolean DEBUG = false;

}
