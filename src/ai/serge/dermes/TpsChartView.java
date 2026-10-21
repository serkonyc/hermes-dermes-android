package ai.serge.dermes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokens/sec chart backed by MPAndroidChart v3.1.0 (pure Java AAR from Maven Central, no
 * Kotlin runtime — ships as ext-src/mpchart.jar, wired in release.sh). This is a real
 * ViewGroup embedding a real chart View properly (addView + layout params), dark-styled.
 * Big current-value label overlays top-left; unit shrinks so text can never overflow the
 * card (the bug from v3.1.x art reviews).
 */
public final class TpsChartView extends FrameLayout {

    private final LineChart chart;
    private final TextView value;
    private final List<Double> samples = new ArrayList<>();

    public TpsChartView(Context c) {
        super(c);
        float d = c.getResources().getDisplayMetrics().density;

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xFF161B22);
        cardBg.setCornerRadius(12 * d);
        cardBg.setStroke(Math.max(1, (int) d), 0xFF30363D);
        setBackground(cardBg);

        chart = new LineChart(c);
        chart.setBackgroundColor(0x00000000);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setScaleEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setPinchZoom(false);
        chart.setTouchEnabled(false);
        chart.setDrawMarkers(false);
        chart.setLogEnabled(false);
        chart.getXAxis().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        YAxis left = chart.getAxisLeft();
        left.setDrawAxisLine(false);
        left.setDrawGridLines(false);
        left.setTextColor(0xFF6E7681);
        left.setTextSize(9f);
        left.setAxisMinimum(0f);
        chart.setViewPortOffsets(34 * d, 26 * d, 10 * d, 6 * d);

        value = new TextView(c);
        value.setTextSize(15f);
        value.setTextColor(0xFFFFFFFF);
        value.setSingleLine(true);
        value.setPadding((int) (12 * d), (int) (8 * d), (int) (8 * d), 0);
        value.setGravity(Gravity.START);

        addView(chart, new LayoutParams(-1, -1));
        addView(value, new LayoutParams(-2, -2));
    }

    public void addSample(double tps) {
        samples.add(Math.max(0, tps));
        while (samples.size() > 60) samples.remove(0);
        rebuild();
    }

    private void rebuild() {
        if (samples.size() < 2) return;
        ArrayList<Entry> entries = new ArrayList<>(samples.size());
        for (int i = 0; i < samples.size(); i++) entries.add(new Entry(i, samples.get(i).floatValue()));
        LineDataSet set = new LineDataSet(entries, "tps");
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.18f);
        set.setColor(0xFF58A6FF);
        set.setLineWidth(2f * getResources().getDisplayMetrics().density);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setDrawHighlightIndicators(false);
        set.setDrawFilled(true);
        set.setFillColor(0xFF58A6FF);
        set.setFillAlpha(60);
        chart.setData(new LineData(set));
        chart.notifyDataSetChanged();
        chart.invalidate();

        double cur = samples.get(samples.size() - 1);
        String big = String.format(java.util.Locale.US, "%.1f", cur);
        String unit = " tokens/s";
        value.setText(big + unit);
        value.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        // overflow guard: the whole card minus left axis gutter must fit the label
        int avail = getWidth() > 0 ? getWidth() - (int) (46 * getResources().getDisplayMetrics().density)
                                   : value.getMeasuredWidth();
        if (value.getMeasuredWidth() > avail) {
            value.setText(big + " t/s");
            value.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            if (value.getMeasuredWidth() > avail) value.setText(big);
        }
    }
}
