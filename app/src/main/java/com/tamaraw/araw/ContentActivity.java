package com.tamaraw.araw;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.tamaraw.araw.Constants.chart;
import static com.tamaraw.araw.Constants.chartButtons;
import static com.tamaraw.araw.Constants.chartType;
import static com.tamaraw.araw.Constants.data;
import static com.tamaraw.araw.Constants.dataset;
import static com.tamaraw.araw.Constants.testAlarm;

public class ContentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);
        try {
            setupChartButtons();
            initGraph();
            startReading();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initGraph() {
        Constants.chart = findViewById(R.id.dataChart);
        Constants.chart.setTouchEnabled(false);
        Constants.chart.getDescription().setEnabled(false);
        XAxis axis =  Constants.chart.getXAxis();
        axis.setPosition(XAxis.XAxisPosition.BOTTOM);
        axis.setLabelCount(0, true);
        setupGraph();
        resetGraph(DataType.Temperature);
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
                        if (dp.getCarbonMonoxide()) {
                            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                            final Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                            r.play();
                            new AlertDialog.Builder(ContentActivity.this)
                                    .setTitle(getString(R.string.warning))
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            r.stop();
                                        }
                                    }).setCancelable(false)
                                    .show();
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
        for (int i = 0; i < 5; i++) {
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
//        chart.setVisibleYRange(chartType.min, chartType.max, YAxis.AxisDependency.LEFT);
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
//        dataset.removeEntry(0);
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
            Constants.coLabel.setVisibility(View.VISIBLE);
            Constants.coStatusIcon.setVisibility(View.VISIBLE);
            Constants.testAlarm.setVisibility(View.VISIBLE);
        } else {
            Constants.chartType = dt;
            Constants.chart.setVisibility(View.VISIBLE);
            Constants.dataLabel.setVisibility(View.VISIBLE);
            Constants.coLabel.setVisibility(View.GONE);
            Constants.coStatusIcon.setVisibility(View.GONE);
            Constants.testAlarm.setVisibility(View.GONE);
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

        int i = 0;
        for (ImageButton b : chartButtons) {
            final int index = i;
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    resetGraph(DataType.values()[index]);
                    Toast.makeText(ContentActivity.this, "Changed to " + Constants.chartType.getName(), Toast.LENGTH_SHORT).show();
                }
            });
            i++;
        }

        Constants.dataLabel = findViewById(R.id.dataLabel);
        Constants.coLabel = findViewById(R.id.coLabel);
        Constants.coStatusIcon = findViewById(R.id.coStatus);
        Constants.testAlarm = findViewById(R.id.testAlarm);

        testAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final ImageView status = findViewById(R.id.coStatus);
                Constants.coStatus = true;
                status.setImageResource(R.mipmap.co_bad);
                status.invalidate();
                status.refreshDrawableState();
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                final Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                new AlertDialog.Builder(ContentActivity.this)
                        .setTitle(getString(R.string.warning))
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                r.stop();
                                Constants.coStatus = false;
                                status.setImageResource(R.mipmap.co_good);
                                status.invalidate();
                                status.refreshDrawableState();
                            }
                        }).setCancelable(false)
                        .show();
            }
        });
    }
}
