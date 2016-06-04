package ru.spbstu.dis.ui.emergency;

import com.google.common.net.HostAndPort;
import com.typesafe.config.Config;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.slf4j.LoggerFactory;
import ru.spbstu.dis.ConfigProvider;
import ru.spbstu.dis.opc.client.api.OpcClientApiFactory;
import ru.spbstu.dis.opc.client.api.opc.access.OpcAccessApi;
import ru.spbstu.dis.opc.client.api.opc.access.Tag;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FilterStationEmergencyPredictionFoulBlockage {
  private static final org.slf4j.Logger LOGGER = LoggerFactory
      .getLogger(FilterStationEmergencyPredictionFoulBlockage.class);

  private final OpcAccessApi opcAccessApi;

  static ArrayList notifications = new ArrayList();

  static String filter_TP_1M7 = Tag.TAG_TO_ID_MAPPING.get(Tag
      .FILT_ControlPanel_WARNING);

  static String filter_open_rev_valve = Tag.TAG_TO_ID_MAPPING.get(Tag.FILT_open_rev_valve);
  static String filter_open_rev_pump = Tag.TAG_TO_ID_MAPPING.get(Tag
                                                                      .FILT_ControlPanel_downstream_station_pump_P102_on);

  static String filter_p102 = Tag.TAG_TO_ID_MAPPING.get(Tag
                                                            .FILT_pump_102_on);

  static double filter_fake_risk_value = 0d;

  static boolean filter_fake_active_flag = true;

  static JList actionsFinishedList;

  static JLabel progressText = new JLabel();
  static DefaultListModel listModel = new DefaultListModel();
  static  JLabel picLabel;
  public FilterStationEmergencyPredictionFoulBlockage(final OpcAccessApi opcAccessApi) {
    this.opcAccessApi = opcAccessApi;
  }

  public static void main(String[] args) {

    try {
      for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (Exception e) {
      // If Nimbus is not available, you can set the GUI to another look and feel.
    }
    FilterStationEmergencyPredictionFoulBlockage emergencyPredictionWindow =
        new FilterStationEmergencyPredictionFoulBlockage(createOpcApi());
    emergencyPredictionWindow.initMeterChart();
    //need to be called last!
    emergencyPredictionWindow.composeAllChartsIntoOne();

    emergencyPredictionWindow.runSimulation();
  }

  private static OpcAccessApi createOpcApi() {
    final Config config = new ConfigProvider().get().resolve();
    final Config opcAccessApiConf = config.getConfig("http.opc.client");
    final String host = opcAccessApiConf.getString("host");
    final int port = opcAccessApiConf.getInt("port");
    final HostAndPort hostAndPort = HostAndPort.fromParts(host, port);
    LOGGER.warn("Opc access api uses {} to connect", hostAndPort);
    return OpcClientApiFactory.createOpcAccessApi(hostAndPort);
  }

  private void initMeterChart() {
    final MeterChart demo = new MeterChart("Вероятность НС на фильтре");

    Thread th = new Thread(() -> {
      while (true) {
        double max = filter_fake_risk_value;
        demo.setValue(max);

        demo.getDataset().setValue(max);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
    th.start();
    final JPanel titlePanel = new JPanel();
    titlePanel.setLayout(new FlowLayout());
    titlePanel.add(demo.getChartPanel());
    closenessChartFrame.add(titlePanel);
  }

  private void composeAllChartsIntoOne() {

    Thread th = new Thread(() -> {
      while (true) {
        //demo.setLastValue((MAX_TEMPERATURE - temperatureGrowthVal) / MAX_TEMPERATURE);
        filter_fake_risk_value += 0.2 * new Random().nextDouble();
        closenessChartFrame.setLastValue(filter_fake_risk_value);
        closenessChartFrame.getSeries().add(new Millisecond(), closenessChartFrame.getLastValue());
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
    th.start();
    JLabel esType = new JLabel("<html>Засор фильтра<br>(механический засор)</html>",
                               SwingConstants.CENTER);
    Map<TextAttribute, Integer> fontAttributes = new HashMap<TextAttribute, Integer>();
    fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
    Font boldUnderline = new Font("Tachoma", Font.BOLD, 22).deriveFont(fontAttributes);
    esType.setFont(boldUnderline);
    final JPanel titlePanel = new JPanel(new FlowLayout());
    titlePanel.add(esType, BorderLayout.CENTER);
    closenessChartFrame.add(titlePanel);
    XYPlot plot = (XYPlot) closenessChartFrame.getChartPanel().getChart().getPlot();
    XYLineAndShapeRenderer renderer0 = new XYLineAndShapeRenderer();
    renderer0.setBaseShapesVisible(false);
    XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
    renderer1.setBaseShapesVisible(false);
    plot.setRenderer(0, renderer0);
    plot.getRendererForDataset(plot.getDataset(0)).setSeriesPaint(0, Color.red);
    plot.getRenderer().setSeriesPaint(0, Color.red);

    SwingUtilities.invokeLater(() -> {

      final JPanel actionRecoms = new JPanel(new FlowLayout());
      JLabel actionRecommLabel = new JLabel("Рекомендуемые действия");
      Font boldUnderlineAction = new Font("Tachoma", Font.BOLD, 15).deriveFont(fontAttributes);
      actionRecommLabel.setFont(boldUnderlineAction);
      actionRecoms.add(actionRecommLabel);
      closenessChartFrame.add(actionRecoms);

      final String[] actions = {"1. Отключить подачу жидкости", "2. Закрыть выпускной клапан",
          "3. Открыть реверсный клапан", "4. Включить резервный насос",
          "5. Задать обратное направление течения жидкости"};
      JList recomActions = new JList(actions);

      final JPanel actionsPanel = new JPanel(new FlowLayout());
      actionsPanel.setBorder(BorderFactory.createCompoundBorder());
      actionsPanel.add(recomActions, BorderLayout.CENTER);
      closenessChartFrame.add(actionsPanel);

      final JPanel actionOutput = new JPanel(new FlowLayout());
      JLabel esTypeAction = new JLabel("<html>ОТРАБОТКА НС:<br>Очистка фильтра</html>", SwingConstants.CENTER);
      Font boldUnderlineBig = new Font("Tachoma", Font.BOLD, 18).deriveFont(fontAttributes);
      esTypeAction.setFont(boldUnderlineBig);
      actionOutput.add(esTypeAction);
      closenessChartFrame.add(actionOutput);

      final JPanel statePanel = new JPanel(new FlowLayout());
      JLabel stateLbl = new JLabel("Текущее состояние фильтра:");
      stateLbl.setFont(new Font("Tachoma", Font.PLAIN, 10));
      statePanel.add(stateLbl);
      closenessChartFrame.add(statePanel);

      JPanel finishedActionsPnl = new JPanel(new FlowLayout());
      BufferedImage myPicture = null;
      try {
        myPicture = ImageIO.read(
            FilterStationEmergencyPredictionFoulBlockage.class.getResource("/pump_inactive.png"));
      } catch (IOException e) {
        e.printStackTrace();
      }
      picLabel = new JLabel(new ImageIcon(myPicture));
      finishedActionsPnl.add(picLabel);
      picLabel.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));
      picLabel.add(new JLabel("        "));
      picLabel.add(new JLabel("       "));
      picLabel.add(progressText);
      actionsFinishedList = new JList(listModel);
      actionsFinishedList.setSize(60, 80);
      finishedActionsPnl.add(actionsFinishedList);

      closenessChartFrame.add(finishedActionsPnl);
      closenessChartFrame.addDecisionsAndCloseButton();

      closenessChartFrame.pack();
      closenessChartFrame.setVisible(true);
    });
  }

  private void runSimulation() {

    Thread th = new Thread(() -> {

      try {
        getDataFromOPC();
      } catch (InterruptedException e) {
        LOGGER.error("Error during data retrieving", e);
      }
    });
    th.start();
  }

  private void getDataFromOPC()
  throws InterruptedException {
    while (true) {
      opcAccessApi.writeValueForTag(filter_p102, Boolean.TRUE); //water filtering

      Thread.sleep(1000);
      if (filter_fake_risk_value > 0.3d) {
        notifier(String.format("Вероятность НС на ст.фильтр. =%s " + "->\n" +
                    "Рекомендуемое действие=%s",
                "СРЕДНЯЯ", "Проверка станции", 0.1),
            0.3);
        opcAccessApi.writeValueForTag(filter_TP_1M7, Boolean.TRUE); //Warning
      }
      else {
        opcAccessApi.writeValueForTag(filter_TP_1M7,  Boolean.FALSE); //warning
      }


      if (filter_fake_risk_value > 0.9d) {

        opcAccessApi.writeValueForTag(filter_p102, Boolean.FALSE); //water filtering
        BufferedImage myPicture = null;
        try {
          myPicture = ImageIO.read(
              FilterStationEmergencyPredictionOldFilterBlockage.class
                  .getResource("/pump_active.png"));
        } catch (IOException e) {
          e.printStackTrace();
        }

        picLabel.setIcon(new ImageIcon(myPicture));
        progressText.setText("0%");
        listModel.addElement("<html>1.Отключить подачу<br> жидкости</html>");
        notifier(String.format("Вероятность НС на ст.фильтр. =%s " + "->\n" +
                    "Рекомендуемое действие=%s",
                "ВЫСОКАЯ", "Сброс давления в фильтре, отключение насосов", 0.1),
            filter_fake_risk_value);
        picLabel.updateUI();
        closenessChartFrame.revalidate();
        closenessChartFrame.repaint();
        progressText.setText("20%");
        listModel.addElement("<html>1.Отключить подачу<br>жидкости</html>");


        opcAccessApi.writeValueForTag(filter_open_rev_valve, Boolean.TRUE);
        listModel.addElement("<html>2.Закрыть выпускной<br>клапан</html>");
        progressText.setText("50%");
        opcAccessApi.writeValueForTag(filter_open_rev_pump, Boolean.TRUE);
        listModel.addElement("<html>3.Открыть реверсный<br>клапан</html>");
        Thread.sleep(10000);
        listModel.addElement("<html>4.Включить резервный<br>насос</html>");
        opcAccessApi.writeValueForTag(filter_open_rev_pump, Boolean.FALSE);
        opcAccessApi.writeValueForTag(filter_open_rev_valve, Boolean.FALSE);
        progressText.setText("70%");
        Thread.sleep(2000);
        listModel.addElement("<html>5.Задать обратное<br>направление</html>" +
            "течения<br>жидкости</html>");
        opcAccessApi.writeValueForTag(filter_p102, Boolean.TRUE);

        Thread.sleep(10000);
        progressText.setText("100%");
        try {
          myPicture = ImageIO.read(FilterStationEmergencyPredictionFoulBlockage
              .class.getResource("/pump_inactive.png"));
        } catch (IOException e) {
          e.printStackTrace();
        }
        picLabel.setIcon(new ImageIcon(myPicture));


        opcAccessApi.writeValueForTag(filter_p102, Boolean.FALSE);
        notifier(String.format("Вероятность НС на ст.фильтр. =%s " + "->\n" +
                    "Рекомендуемое действие=%s",
                "НИЗКАЯ", "Штатный режим", 0.1),
            0.1);
        filter_fake_risk_value = 0.3d;
        filter_fake_active_flag = false;
        opcAccessApi.writeValueForTag(filter_TP_1M7,  Boolean.FALSE); //warning
        Thread.sleep(1000);
        return;
      }
    }
  }

  private static void notifier(String term, double dangerLevel) {

    if (dangerLevel > 0.5d) {
      ImageIcon icon = new ImageIcon(FilterStationEmergencyPredictionFoulBlockage.class.getResource("/alarm.png"));
      showErrorNotif(term, new Color(249, 78, 30), icon);
    }
    if (dangerLevel > 0.0d && dangerLevel <= 0.3d) {
      ImageIcon icon = new ImageIcon(FilterStationEmergencyPredictionFoulBlockage.class.getResource("/info.png"));
      showErrorNotif(term, new Color(127, 176, 72), icon);
    }
    if (dangerLevel > 0.3d && dangerLevel <= 0.5d) {
      ImageIcon icon = new ImageIcon(FilterStationEmergencyPredictionFoulBlockage.class.getResource("/warning.png"));
      showErrorNotif(term, new Color(249, 236, 100), icon);
    }
  }

  private static void showErrorNotif(String term, Color color, ImageIcon icon) {
    try {
      Thread.sleep(2000);
    } catch (InterruptedException ex) {
      Logger.getLogger(PopupTester.class.getName()).log(Level.SEVERE, null, ex);
    }
    notifications.add(term);
      closenessChartFrame.getList().setListData(notifications.toArray());
    closenessChartFrame.getList().updateUI();
    closenessChartFrame.repaint();
    try {
      Thread.sleep(1000);
    } catch (InterruptedException ex) {
      Logger.getLogger(PopupTester.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private static DynamicDataChart closenessChartFrame = new DynamicDataChart(
      "Вероятность НС засора фильтра");


  private static final int MAX_FLOW_SPEED = 50;
}