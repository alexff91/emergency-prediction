package ru.spbstu.dis.ui.emergency;

import com.google.common.net.HostAndPort;
import com.typesafe.config.Config;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.slf4j.LoggerFactory;
import ru.spbstu.dis.ConfigProvider;
import ru.spbstu.dis.opc.client.api.OpcClientApiFactory;
import ru.spbstu.dis.opc.client.api.opc.access.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;

public class FilterStationEmergencyPredictionFilterDestructionAfterOuterValve {
  private static final org.slf4j.Logger LOGGER = LoggerFactory
      .getLogger(FilterStationEmergencyPredictionFilterDestructionAfterOuterValve.class);

  private final OpcAccessApi opcAccessApi;

  static ArrayList notifications = new ArrayList();

  static String downstream = Tag.TAG_TO_ID_MAPPING.get(Tag.REACTOR_DOWNSTREAM_ON);

  static String reactorCooler = Tag.TAG_TO_ID_MAPPING.get(Tag.REACTOR_ControlPanel_Mixing_on);

  static String reactorHeater = Tag.TAG_TO_ID_MAPPING
      .get(Tag.REACTOR_ControlPanel_mixing_pump_P201_on);


  static String filter_TP_1M6 = Tag.TAG_TO_ID_MAPPING.get(Tag
      .FILT_ControlPanel_downstream_valve_V103_on);

  static String filter_TP_1M7 = Tag.TAG_TO_ID_MAPPING.get(Tag
      .FILT_ControlPanel_WARING);

  static String filter_p101 = Tag.TAG_TO_ID_MAPPING.get(Tag
      .FILT_pump_101_on);

  static String filter_p102 = Tag.TAG_TO_ID_MAPPING.get(Tag
      .FILT_pump_102_on);

  static double filter_fake_risk_value = 0d;

  static boolean filter_fake_active_flag = true;

  public FilterStationEmergencyPredictionFilterDestructionAfterOuterValve(
      final OpcAccessApi opcAccessApi) {
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
    FilterStationEmergencyPredictionFilterDestructionAfterOuterValve
        filterStationEmergencyPredictionFilterDestructionAfterOuterValve =
        new FilterStationEmergencyPredictionFilterDestructionAfterOuterValve(createOpcApi());
    filterStationEmergencyPredictionFilterDestructionAfterOuterValve.initMeterChart();
    //need to be called last!s
    filterStationEmergencyPredictionFilterDestructionAfterOuterValve.composeAllChartsIntoOne();

    filterStationEmergencyPredictionFilterDestructionAfterOuterValve.runSimulation();
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
    final MeterChart demo = new MeterChart("Вероятность НС на станциях");

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
        if (filter_fake_active_flag) {
          filter_fake_risk_value += 0.1 * new Random().nextDouble();
        }
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

    JLabel esType = new JLabel("ТИП НС1");
    Map<TextAttribute, Integer> fontAttributes = new HashMap<TextAttribute, Integer>();
    fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
    Font boldUnderline = new Font("Tachoma", Font.BOLD, 28).deriveFont(fontAttributes);
    esType.setFont(boldUnderline);
    final JPanel titlePanel = new JPanel(new FlowLayout());
    titlePanel.add(esType, BorderLayout.CENTER);
    closenessChartFrame.add(titlePanel);
    XYPlot plot = (XYPlot) closenessChartFrame.getChartPanel().getChart().getPlot();
    XYLineAndShapeRenderer renderer0 = new XYLineAndShapeRenderer();
    renderer0.setBaseShapesVisible(false);
    plot.setRenderer(0, renderer0);
    plot.getRendererForDataset(plot.getDataset(0)).setSeriesPaint(0, new Color((int)(Math.random() * 0x1000000)));
    plot.getRenderer().setSeriesPaint(0, new Color((int)(Math.random() * 0x1000000)));

    SwingUtilities.invokeLater(() -> {

      final JPanel actionRecoms = new JPanel(new FlowLayout());
      JLabel actionRecommLabel = new JLabel("Рекомендуемые действия");
      Font boldUnderlineAction = new Font("Tachoma", Font.BOLD, 15).deriveFont(fontAttributes);
      actionRecommLabel.setFont(boldUnderlineAction);
      actionRecoms.add(actionRecommLabel);
      closenessChartFrame.add(actionRecoms);

      final String[] actions = {"1. Отключить станцию", "2. Выключить насос",
          "3. Проверить соединение"};
      JList recomActions = new JList(actions);

      final JPanel actionsPanel = new JPanel(new FlowLayout());
      actionsPanel.setBorder(BorderFactory.createCompoundBorder());
      actionsPanel.add(recomActions, BorderLayout.CENTER);
      closenessChartFrame.add(actionsPanel);

      final JPanel actionOutput = new JPanel(new FlowLayout());
      JLabel esTypeAction = new JLabel("ОТРАБОТКА НС1");
      Font boldUnderlineBig = new Font("Tachoma", Font.BOLD, 25).deriveFont(fontAttributes);
      esTypeAction.setFont(boldUnderlineBig);
      actionOutput.add(esTypeAction);
      closenessChartFrame.add(actionOutput);

      final JPanel statePanel = new JPanel(new FlowLayout());
      JLabel stateLbl = new JLabel("Текущее состояние зоны:");
      stateLbl.setFont(new Font("Tachoma", Font.PLAIN, 10));
      statePanel.add(stateLbl);
      closenessChartFrame.add(statePanel);

      JPanel finishedActionsPnl = new JPanel(new FlowLayout());
      BufferedImage myPicture = null;
      try {
        myPicture = ImageIO.read(FilterStationEmergencyPredictionFilterDestructionAfterOuterValve.class.getResource("/filter.png"));
      } catch (IOException e) {
        e.printStackTrace();
      }
      JLabel picLabel = new JLabel(new ImageIcon(myPicture));
      finishedActionsPnl.add(picLabel);

      final String[] actionsFinished = {"1 - выполнено",
          "2 - выполнено",
          "3 - ..."};
      JList actionsFinishedList = new JList(actionsFinished);
      finishedActionsPnl.add(actionsFinishedList);

      closenessChartFrame.add(finishedActionsPnl);
      closenessChartFrame.addDecisionsAndCloseButton();

      closenessChartFrame.pack();
      closenessChartFrame.setLocation(1050,0);
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
    //int k=0;
    while (true) {

      //if(k==0) {
      opcAccessApi.writeValueForTag(filter_p102, Boolean.TRUE);
      //  k=1;
      // }
      Thread.sleep(2000);
      if (filter_fake_risk_value > 0.3d) {
        notifier(String.format("Вероятность НС на ст.фильтр. =%s " + "->\n" +
                    "Рекомендуемое действие=%s",
                "СРЕДНЯЯ", "Проверка станции", 0.1),
            0.3);
        opcAccessApi.writeValueForTag(filter_TP_1M7, Boolean.TRUE);
      }


      if (filter_fake_risk_value > 0.9d) {
        filter_fake_risk_value = 0.3d;
        filter_fake_active_flag = false;
        opcAccessApi.writeValueForTag(filter_p102, Boolean.FALSE);

        notifier(String.format("Вероятность НС на ст.фильтр. =%s " + "->\n" +
                    "Рекомендуемое действие=%s",
                "ВЫСОКАЯ", "Сброс давления в фильтре, отключение насосов", 0.1),
            filter_fake_risk_value);

        opcAccessApi.writeValueForTag(filter_TP_1M6, Boolean.TRUE);
        opcAccessApi.writeValueForTag(filter_TP_1M6, Boolean.FALSE);
        opcAccessApi.writeValueForTag(filter_p101, Boolean.TRUE);
        opcAccessApi.writeValueForTag(filter_TP_1M7, Boolean.FALSE);
        Thread.sleep(5000);
        opcAccessApi.writeValueForTag(filter_p101, Boolean.FALSE);
        opcAccessApi.writeValueForTag(filter_p102, Boolean.TRUE);
        Thread.sleep(10000);
        opcAccessApi.writeValueForTag(filter_p102, Boolean.FALSE);
        notifier(String.format("Вероятность НС на ст.фильтр. =%s " + "->\n" +
                    "Рекомендуемое действие=%s",
                "НИЗКАЯ", "Штатный режим", 0.1),
            0.1);
        return;
      }
    }
  }

  private static void notifier(String term, double dangerLevel) {

    if (dangerLevel > 0.5d) {
      ImageIcon icon = new ImageIcon(FilterStationEmergencyPredictionFilterDestructionAfterOuterValve.class.getResource("/alarm.png"));
      showErrorNotif(term, new Color(249, 78, 30), icon);
    }
    if (dangerLevel > 0.0d && dangerLevel <= 0.3d) {
      ImageIcon icon = new ImageIcon(FilterStationEmergencyPredictionFilterDestructionAfterOuterValve.class.getResource("/info.png"));
      showErrorNotif(term, new Color(127, 176, 72), icon);
    }
    if (dangerLevel > 0.3d && dangerLevel <= 0.5d) {
      ImageIcon icon = new ImageIcon(FilterStationEmergencyPredictionFilterDestructionAfterOuterValve.class.getResource("/warning.png"));
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
      "Вероятность НС на станции смешивания");

  private boolean showUserDecisionDialog() {
    // show a joptionpane dialog using showMessageDialog
    Object[] options = {"Включить миксер", "Охладить реактор", "Охладить на 20",
        "Остановить нагрев"};
    int n = JOptionPane.showOptionDialog(closenessChartFrame, "Принятие решений?", "Вывод из НС",
        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
    if (n == 1) {
      opcAccessApi.writeValueForTag(downstream, true);
    }
    if (n == 2) {
    }
    if (n == 3) {
      opcAccessApi.writeValueForTag(reactorCooler, false);
      opcAccessApi.writeValueForTag(reactorHeater, false);
      return true;
    }
    if (n == 0) {
      opcAccessApi.writeValueForTag(reactorCooler, true);
    }
    return false;
  }

  private static final int MAX_FLOW_SPEED = 50;
}