package com.tamaraw.araw;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.tamaraw.araw.Constants.btnSMSTest;
import static com.tamaraw.araw.Constants.chart;
import static com.tamaraw.araw.Constants.chartButtons;
import static com.tamaraw.araw.Constants.chartType;
import static com.tamaraw.araw.Constants.coDialogOpen;
import static com.tamaraw.araw.Constants.coSnooze;
import static com.tamaraw.araw.Constants.data;
import static com.tamaraw.araw.Constants.dataset;
import static com.tamaraw.araw.Constants.testAlarm;

public class ContentActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_DENIED) {
                Log.d("Permission", "Missing permission, requesting now.");
                String[] permissions = {Manifest.permission.SEND_SMS};
                requestPermissions(permissions, PERMISSION_REQUEST_CODE);
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            }
        }
        try {
            setupChartButtons();
            initGraph();
            if (!Constants.DEBUG) {
                startReading();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initGraph() {
        Constants.chart = findViewById(R.id.dataChart);
        Constants.chart.setTouchEnabled(false);
        Constants.chart.getDescription().setEnabled(false);
        XAxis axis = Constants.chart.getXAxis();
        axis.setPosition(XAxis.XAxisPosition.BOTTOM);
        axis.setLabelCount(0, true);
        setupGraph();
        resetGraph(DataType.Temperature);
    }

    private void sendSMS(DataPacket dp) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Dangerous weather conditions detected! ");
        sb.append("Temperature: ");
        sb.append(dp.getTemp());
        sb.append(DataType.Temperature.getUnit());
        sb.append(", ");
        sb.append("Humidity: ");
        sb.append(dp.getHumidity());
        sb.append(DataType.Humidity.getUnit());
        sb.append(", ");
        sb.append("Heat Index: ");
        sb.append(dp.getHeatIndex());
        sb.append(DataType.HeatIndex.getUnit());
        sb.append(", ");
        sb.append("Air Pressure: ");
        sb.append(dp.getPressure());
        sb.append(DataType.Pressure.getUnit());
        sb.append(", ");
        sb.append("Carbon Monoxide: ");
        sb.append(dp.getCarbonMonoxide() ? "Danger" : "Safe");
        String dataText = sb.toString();

        System.out.println(dataText);
        try {
            for (String contact : Constants.contacts) {
                if (!contact.equals("")) {
                    sendSMS(contact, dataText);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));


        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;

                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();

        ArrayList<String> parts = sms.divideMessage(message);
        ArrayList<PendingIntent> sendList = new ArrayList<>();
        sendList.add(sentPI);

        ArrayList<PendingIntent> deliverList = new ArrayList<>();
        deliverList.add(deliveredPI);

        sms.sendMultipartTextMessage(phoneNumber, null, parts, sendList, deliverList);
    }


    private void alarmCO() {
//        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
//        final Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
//        r.play();
        Constants.coDialogOpen = true;
        final MediaPlayer player = MediaPlayer.create(ContentActivity.this, R.raw.alarm);
        player.start();
        new AlertDialog.Builder(ContentActivity.this)
                .setTitle(getString(R.string.warning))
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        r.stop();
                        player.stop();
                        Constants.coStatus = false;
                        ImageView status = findViewById(R.id.coStatus);
                        status.setImageResource(R.mipmap.co_good);
                        status.invalidate();
                        status.refreshDrawableState();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Snoozed for 30 seconds", Toast.LENGTH_LONG).show();
                            }
                        });
                        Constants.coDialogOpen = false;
                        Constants.coSnooze = true;
                    }
                }).setCancelable(false)
                .show();

        final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
        exec.schedule(new Runnable() {
            @Override
            public void run() {
                Constants.coSnooze = false;
            }
        }, 30, TimeUnit.SECONDS);
    }

    private void startReading() {
        final Thread thread = new Thread() {
            @Override
            public void run() {
                ContentActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ContentActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
                    }
                });
                while (true) {
                    try {
                        System.out.println("Started reading");
                        String line = Constants.btReader.readLine();
                        System.out.println(line);
                        String[] tokens = line.split(" ");
                        double[] data = new double[tokens.length]; // temp hum heatIndex pressure carbon_monoxide limit_reached
                        for (int i = 0; i < tokens.length; i++) {
                            try {
                                data[i] = Double.parseDouble(tokens[i]);
                            } catch (NumberFormatException e) {
                                data[i] = 0;
                            }
                        }
                        DataPacket dp = new DataPacket(data);
                        double val = 0;
                        Constants.coStatus = dp.getCarbonMonoxide();
                        if (dp.danger && !Constants.smsSnooze) {
                            sendSMS(dp);
                            Constants.smsSnooze = true;
                            final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
                            exec.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    Constants.smsSnooze = false;
                                }
                            }, 60, TimeUnit.SECONDS);
                        }
                        if (dp.getCarbonMonoxide() && !Constants.coSnooze && !coDialogOpen) {
                            alarmCO();
                        }
                        if (chartType == DataType.CarbonMonoxide) {
                            if (dp.getCarbonMonoxide() != Constants.coStatus) {
                                ImageView status = findViewById(R.id.coStatus);
                                status.setImageResource(Constants.coStatus ? R.mipmap.co_bad : R.mipmap.co_good);
                                status.invalidate();
                                status.refreshDrawableState();
                            }
                        } else {
                            if (chartType == DataType.Humidity) {
                                val = dp.getHumidity();
                            } else if (chartType == DataType.Temperature) {
                                val = dp.getTemp();
                            } else if (chartType == DataType.HeatIndex) {
                                val = dp.getHeatIndex();
                            } else if (chartType == DataType.Pressure) {
                                val = dp.getPressure();
                            }
                            final float vf = (float) val;
                            final DataType ct = chartType;
                            try {
                                Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
                                        Constants.dataLabel.setText(ct.getName() + ": " + String.format(Locale.getDefault(), "%.2f", vf) + ct.getUnit());
                                        dataset.removeEntry(0);
                                        updateGraph(new Entry(Constants.lastIndex++, vf));
                                    }
                                };
                                runOnUiThread(r);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            System.out.println("Finished reading");
                            sleep(500);
                        }
                    } catch (Exception e) {
                        System.out.println("Exception occured.");
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
    }

    private void setupGraph() {
        IValueFormatter dataFormatter = new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                String out = String.format(Locale.getDefault(), "%.2f", value) + chartType.getUnit();
                return out;
            }
        };
        List<Entry> entries = new ArrayList<>();
//        Random r = new Random();
        for (int i = 0; i < Constants.dataSize; i++) {
            entries.add(new Entry(Constants.lastIndex++, 0));
        }
        String title = chartType.getName() + " " + chartType.getUnit();
        int chartColor = ContextCompat.getColor(getApplicationContext(), R.color.chartColor);
        int color = ContextCompat.getColor(getApplicationContext(), R.color.dataPointColor);

        dataset = new LineDataSet(entries, title);
        dataset.setColor(chartType.getColor(this));
        dataset.setLabel(title);
        dataset.setCircleColor(color);
        dataset.setCircleColorHole(color);

        data = new LineData(dataset);
        data.setValueTextColor(chartColor);
        data.setValueFormatter(dataFormatter);

        chart.setData(data);
        chart.getXAxis().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(chartType.min);
        chart.getAxisLeft().setAxisMaximum(chartType.max);
        chart.getAxisRight().setAxisMinimum(chartType.min);
        chart.getAxisRight().setAxisMaximum(chartType.max);
        chart.getAxisRight().setTextSize(8);
        chart.getAxisLeft().setTextSize(8);
        chart.getAxisLeft().setTextColor(chartColor);
        chart.getAxisRight().setTextColor(chartColor);
        chart.setBorderColor(chartColor);
        chart.getLegend().setTextColor(chartColor);
    }

    private void updateGraph(Entry entry) {
        dataset.addEntry(entry);
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void resetGraph(DataType dt) {
        if (dt == DataType.CarbonMonoxide) {
            Constants.chartType = dt;
            Constants.chart.setVisibility(View.GONE);
            Constants.dataLabel.setVisibility(View.GONE);
            Constants.settingsContainer.setVisibility(View.GONE);
            Constants.coLabel.setVisibility(View.VISIBLE);
            Constants.coStatusIcon.setVisibility(View.VISIBLE);
            Constants.testAlarm.setVisibility(View.VISIBLE);
        } else if (dt == null) {
            Constants.chart.setVisibility(View.GONE);
            Constants.dataLabel.setVisibility(View.GONE);
            Constants.coLabel.setVisibility(View.GONE);
            Constants.coStatusIcon.setVisibility(View.GONE);
            Constants.testAlarm.setVisibility(View.GONE);
            Constants.settingsContainer.setVisibility(View.VISIBLE);
        } else {
            Constants.chartType = dt;
            Constants.chart.setVisibility(View.VISIBLE);
            Constants.dataLabel.setVisibility(View.VISIBLE);
            Constants.coLabel.setVisibility(View.GONE);
            Constants.coStatusIcon.setVisibility(View.GONE);
            Constants.testAlarm.setVisibility(View.GONE);
            Constants.settingsContainer.setVisibility(View.GONE);
            chart.clearValues();
            Constants.dataLabel.setText(dt.getName() + ": ");
            setupGraph();
        }
    }

    private void setupChartButtons() {
        chartButtons[0] = findViewById(R.id.buttonTemperature);
        chartButtons[1] = findViewById(R.id.buttonHumidity);
        chartButtons[2] = findViewById(R.id.buttonHeatIndex);
        chartButtons[3] = findViewById(R.id.buttonPressure);
        chartButtons[4] = findViewById(R.id.buttonCO);
        chartButtons[5] = findViewById(R.id.buttonSettings);

        int i = 0;
        for (ImageButton b : chartButtons) {
            final int index = i;
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (index == 5) {
                        resetGraph(null);
                    } else {
                        resetGraph(DataType.values()[index]);
                        Toast.makeText(ContentActivity.this, "Changed to " + Constants.chartType.getName(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            i++;
        }

        Constants.dataLabel = findViewById(R.id.dataLabel);
        Constants.coLabel = findViewById(R.id.coLabel);
        Constants.coStatusIcon = findViewById(R.id.coStatus);
        Constants.testAlarm = findViewById(R.id.testAlarm);
        Constants.btnSavePhoneNum = findViewById(R.id.btnSavePhoneNum);
        Constants.inputs[0] = findViewById(R.id.inputPhoneNum1);
        Constants.inputs[1] = findViewById(R.id.inputPhoneNum2);
        Constants.inputs[2] = findViewById(R.id.inputPhoneNum3);
        Constants.inputs[3] = findViewById(R.id.inputPhoneNum4);
        Constants.inputs[4] = findViewById(R.id.inputPhoneNum5);
        Constants.btnSMSTest = findViewById(R.id.btnTestSMS);
        Constants.settingsContainer = findViewById(R.id.settingsContainer);

        testAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!coDialogOpen && !coSnooze) {
                    final ImageView status = findViewById(R.id.coStatus);
                    Constants.coStatus = true;
                    status.setImageResource(R.mipmap.co_bad);
                    status.invalidate();
                    status.refreshDrawableState();
                    alarmCO();
                } else if (coSnooze) {
                    Toast.makeText(getApplicationContext(), "Currently on snooze!", Toast.LENGTH_LONG).show();
                }
            }
        });

        btnSMSTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (String contact : Constants.contacts) {
                    if (!contact.equals("")) {
                        sendSMS(contact, "Araw sample SMS");
                    }
                }
            }
        });

        Constants.btnSavePhoneNum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i = 0; i < Constants.contacts.length; i++) {
                    EditText input = Constants.inputs[i];
                    String contact = input.getText().toString().trim();
                    Constants.contacts[i] = contact;
                    Toast.makeText(getApplicationContext(), "Saved contacts!", Toast.LENGTH_LONG).show();
                }
            }
        });

    }
}
