package com.sweproject.analysis2;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class TrackPlotter extends JFrame {

    public TrackPlotter(String title, Tracks tracks) {
        super(title);

        // Create dataset
        XYSeriesCollection dataset = createDataset(tracks);

        // Create chart
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Track Values",
                "Time",
                "Values",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        // Customize the plot
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        plot.setRenderer(renderer);

        // Create Panel
        ChartPanel panel = new ChartPanel(chart);
        setContentPane(panel);
    }

    private XYSeriesCollection createDataset(Tracks tracks) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (Map.Entry<String, Double[]> entry : tracks.tracks.entrySet()) {
            XYSeries series = new XYSeries(entry.getKey());
            Double[] values = entry.getValue();
            for (int i = 0; i < values.length; i++) {
                series.add(i, values[i]);
            }
            dataset.addSeries(series);
        }

        return dataset;
    }
}