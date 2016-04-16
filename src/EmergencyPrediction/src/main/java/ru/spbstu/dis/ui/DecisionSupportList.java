package ru.spbstu.dis.ui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import javax.swing.*;
import java.awt.*;

/**
 * A demonstration application showing how to create a combined chart using...
 */
public class DecisionSupportList {

  private TimeSeries series;

  private JFrame frame;

  private JList list;

  /**
   * Constructs a new demonstration application.
   * @param title the frame title.
   */
  public DecisionSupportList(final String title) {

    final JFreeChart chart = createCombinedChart("Прирост значения");
    final ChartPanel panel = new ChartPanel(chart, true, true, true, false, true);
    panel.setSize(new java.awt.Dimension(500, 300));
    //        setContentPane(panel);
    frame = new JFrame("Панель контроля нештатных ситуаций");
    frame.setLayout(new GridLayout(2,1));
    list = new JList();
    list.setCellRenderer(new WhiteYellowCellRenderer());
    Object[] data = new Object[10];

    list.setListData(data);
    JPanel p = new JPanel(new GridLayout(2,1));
    p.add(panel);
    p.add(list);

    frame.getContentPane().add(p);

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setBounds(0, 360, 900, 900);
    frame.setVisible(true);
  }

  private static class WhiteYellowCellRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList list, Object value, int index,
        boolean isSelected, boolean cellHasFocus) {
      Component c = super.getListCellRendererComponent(list,
          value,
          index,
          isSelected,
          cellHasFocus);

      if (value != null) {
        if (value.toString()
            .contains(KnowledgeBaseRuleGenerator.lowLevelName)) {
          c.setBackground(Color.green);
        }
        if (value.toString()
            .contains(KnowledgeBaseRuleGenerator.normalLevelName)) {
          c.setBackground(Color.yellow);
        } if (value.toString()
            .contains(KnowledgeBaseRuleGenerator.highLevelName)) {
          c.setBackground(Color.red);
        }
      }
      return c;
    }
  }
  // ****************************************************************************
  // * JFREECHART DEVELOPER GUIDE                                               *
  // * The JFreeChart Developer Guide, written by David Gilbert, is available   *
  // * to purchase from Object Refinery Limited:                                *
  // *                                                                          *
  // * http://www.object-refinery.com/jfreechart/guide.html                     *
  // *                                                                          *
  // * Sales are used to provide funding for the JFreeChart project - please    *
  // * support us so that we can continue developing free software.             *
  // ****************************************************************************

  /**
   * Creates a combined XYPlot chart.
   * @return the combined chart.
   */
  private JFreeChart createCombinedChart(String title) {

    this.series = new TimeSeries(title, Millisecond.class);
    final TimeSeriesCollection dataset = new TimeSeriesCollection(this.series);
    final JFreeChart chart = createChart(dataset, title);

    return chart;
  }

  private JFreeChart createChart(final XYDataset dataset, String valueTitle) {
    final JFreeChart result = ChartFactory.createTimeSeriesChart(
        valueTitle,
        "Время",
        valueTitle,
        dataset,
        true,
        true,
        false
    );
    final XYPlot plot = result.getXYPlot();
    ValueAxis axis = plot.getDomainAxis();
    axis.setAutoRange(true);
    axis.setFixedAutoRange(60000.0);  // 60 seconds
    axis = plot.getRangeAxis();
    axis.setRange(0.0, 100.0);
    return result;
  }

  public JList getList() {
    return list;
  }

  public void setList(JList list) {
    this.list = list;
  }

  public JFrame getFrame() {
    return frame;
  }

  public void setFrame(JFrame frame) {
    this.frame = frame;
  }

  public TimeSeries getSeries() {
    return series;
  }

  public void setSeries(TimeSeries series) {
    this.series = series;
  }

  /**
   * Starting point for the demonstration application.
   * @param args ignored.
   */
  public static void main(final String[] args) {

    final DecisionSupportList demo = new DecisionSupportList("Combined XY Plot Demo 5");
    //        demo.pack();
    //        RefineryUtilities.centerFrameOnScreen(demo);
    //        demo.setVisible(true);

  }
}