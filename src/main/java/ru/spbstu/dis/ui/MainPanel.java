package ru.spbstu.dis.ui;

import com.google.common.util.concurrent.AtomicDouble;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.dcom.da.OPCSERVERSTATUS;
import org.openscada.opc.lib.common.AlreadyConnectedException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.common.NotConnectedException;
import org.openscada.opc.lib.da.AddFailedException;
import org.openscada.opc.lib.da.DataCallback;
import org.openscada.opc.lib.da.DuplicateGroupException;
import org.openscada.opc.lib.da.Group;
import org.openscada.opc.lib.da.Item;
import org.openscada.opc.lib.da.ItemState;
import org.openscada.opc.lib.da.Server;
import org.openscada.opc.lib.da.SyncAccess;
import org.openscada.opc.lib.da.browser.FlatBrowser;
import static ru.spbstu.dis.ui.KnowledgeBaseRuleGenerator.*;

import java.awt.*;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import org.jfree.data.time.Millisecond;

import popup.ssn.NotificationPopup;
import ru.spbstu.dis.ep.data.Tag;
import ru.spbstu.dis.ep.data.opc.OPCDataReader;

import com.fuzzylite.FuzzyLite;
import com.fuzzylite.Op;
import com.fuzzylite.variable.InputVariable;

public class MainPanel {

  static double growthValue;

  static double closenessValue;

  static double tankOverheatClosenessValue;

  static Item reactorTempItemOPC;

  static Item reactorCoolerItemOPC;

  static Item reactorTempSensorOPC;
  public final static Double MAX_TEMPERATURE = 70d;

  static DecisionSupportList decisionSupportList;

  static ArrayList notifications = new ArrayList();
  static AtomicDouble modellingTemperature;

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

    initGrowthValueChart();
    initRiskValueChart();
    initThermometerChart();
    initClosenessValueChart();
    SwingUtilities
        .invokeLater(() -> decisionSupportList = new DecisionSupportList("Decisions List"));
    runSimulation();
  }

  private static void initThermometerChart() {
    final Thermometer demo = new Thermometer("Heat station temperature");

    Thread th = new Thread(() -> {
      while (true) {
        demo.value = growthValue;
        demo.dataset.setValue(growthValue * 10);
        final Millisecond now = new Millisecond();
        System.out.println("Now = " + now.toString());
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
    th.start();
    final JPanel titlePanel = new JPanel(new FlowLayout());
    titlePanel.add(new JLabel("Heating Station control"));
    closenessChartFrame.add(titlePanel);
    closenessChartFrame.addChart(demo.chartPanel);
  }

  private static DynamicDataChart closenessChartFrame = new DynamicDataChart("Closeness Value");

  private static void initClosenessValueChart() {

    Thread th = new Thread(() -> {
      while (true) {
        closenessChartFrame.lastValue = closenessValue;
        final Millisecond now = new Millisecond();
        System.out.println("Now = " + now.toString());
        closenessChartFrame.series.add(new Millisecond(), closenessChartFrame.lastValue);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
    th.start();

    SwingUtilities.invokeLater(() -> {
      closenessChartFrame.addButton();
      closenessChartFrame.pack();
      closenessChartFrame.setVisible(true);
    });
  }

  private static void initGrowthValueChart() {
    final DynamicDataChart demo = new DynamicDataChart("Growth value");

    Thread th = new Thread(() -> {
      while (true) {
        demo.lastValue = growthValue;
        final Millisecond now = new Millisecond();
        System.out.println("Now = " + now.toString());
        demo.series.add(new Millisecond(), demo.lastValue);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
    th.start();

    closenessChartFrame.addChart(demo.chartPanel);
  }

  private static void initRiskValueChart() {
    final DynamicDataChart demo = new DynamicDataChart("Tank Overflow Risk");

    Thread th = new Thread(() -> {
      while (true) {
        demo.lastValue = tankOverheatClosenessValue;
        final Millisecond now = new Millisecond();
        System.out.println("Now = " + now.toString());
        demo.series.add(new Millisecond(), demo.lastValue);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
    th.start();

    closenessChartFrame.addChart(demo.chartPanel);
  }

  private static void runSimulation() {
    generateRulesForOverflowOfTank();
    Thread th = new Thread(() -> {


      try {
        getDataFromOPC(tGrowth, tCloseness, tankOverflowRisk);
      } catch (AlreadyConnectedException e) {
        e.printStackTrace();
      } catch (JIException e) {
        e.printStackTrace();
      } catch (UnknownHostException e) {
        e.printStackTrace();
      } catch (NotConnectedException e) {
        e.printStackTrace();
      } catch (DuplicateGroupException e) {
        e.printStackTrace();
      } catch (AddFailedException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    th.start();
  }

  private static void getDataFromOPC(final InputVariable tGrowth, final InputVariable tCloseness,
      final InputVariable tankOverHeatRisk)
  throws AlreadyConnectedException, JIException, UnknownHostException, NotConnectedException,
  DuplicateGroupException, AddFailedException, InterruptedException {
    final ConnectionInformation ci = new ConnectionInformation();
    ci.setHost("seal-machine1");
    ci.setUser("Administrator");
    ci.setPassword("seal");
    ci.setClsid("6F17505C-4351-46AC-BC1E-CDE34BB53FAA");

    final Server server = new Server(ci, Executors.newSingleThreadScheduledExecutor());
    server.connect();
    OPCSERVERSTATUS serverState = server.getServerState();
    System.out.println("serverState = " + serverState.getBuildNumber());


    SyncAccess syncAccess = new SyncAccess(server, 200);

    String reactorTemperature = Tag.TAG_TO_ID_MAPPING.get(Tag.REACTOR_Controlled_TEMPERATURE);
    syncAccess.addItem(reactorTemperature, new DataCallback() {
      @Override
      public void changed(
          Item item,
          ItemState itemState) {
        System.out.println(item.getId());
        try {
          float objectAsBoolean = itemState.getValue()
              .getObjectAsFloat();
          tCloseness.setInputValue(objectAsBoolean/MAX_TEMPERATURE);
          System.out.println("reactor temperature set = " + objectAsBoolean);
        } catch (JIException e) {
          e.printStackTrace();
        }
      }
    });

    String reactorTemperatureSensor = Tag.TAG_TO_ID_MAPPING
        .get(Tag.REACTOR_Current_Process_Temperature);
    syncAccess.addItem(reactorTemperatureSensor, new DataCallback() {
      @Override
      public void changed(
          Item item,
          ItemState itemState) {
        System.out.println(item.getId());
        try {
          float objectAsBoolean = itemState.getValue()
              .getObjectAsFloat();
          tGrowth.setInputValue(objectAsBoolean/MAX_TEMPERATURE);
          System.out.println("reactor real temperature = " + objectAsBoolean);
        } catch (JIException e) {
          e.printStackTrace();
        }
      }
    });

    String reactorCooler = Tag.TAG_TO_ID_MAPPING.get(Tag.REACTOR_ControlPanel_Mixing_on);
    syncAccess.addItem(reactorCooler, new DataCallback() {
      @Override
      public void changed(
          Item item,
          ItemState itemState) {
        System.out.println(item.getId());
        try {
          boolean objectAsBoolean = itemState.getValue()
              .getObjectAsBoolean();
          System.out.println("reactor real temperature = " + objectAsBoolean);
        } catch (JIException e) {
          e.printStackTrace();
        }
      }
    });

    syncAccess.bind();
    final Group serverObject = server.addGroup("test");

    reactorTempItemOPC = serverObject.addItem(reactorTemperature);

    reactorTempSensorOPC = serverObject.addItem(reactorTemperatureSensor);
    reactorCoolerItemOPC = serverObject.addItem(reactorCooler);

    modellingTemperature = new AtomicDouble();
    while (true) {

      // TODO change to formula of water tank filling
      modellingTemperature.set(modellingTemperature.get() + 0.1d);
      tankOverheatClosenessValue = modellingTemperature.get();
      tCloseness.setInputValue(tankOverheatClosenessValue);
      decisionSupportList.getSeries().add(new Millisecond(), tankOverheatClosenessValue);
      engine.process();

      if (action.highestMembershipTerm(action.getOutputValue()) != null
          && action.highestMembershipTerm(action.getOutputValue()).getName()
          .equals(userDecisionAction)) {
        if (showUserDecisionDialog()) {
          return;
        }
      }
      if (action.highestMembershipTerm(action.getOutputValue()) == null
          || action.highestMembershipTerm(action.getOutputValue()).getName()
          .equals(emergencyStopAction)) {
        // show a joptionpane dialog using showMessageDialog
        JOptionPane.showMessageDialog(closenessChartFrame, "Water Overflow",
            "Emergency situation, Station " + "stopped", JOptionPane.ERROR_MESSAGE);
        modellingTemperature.set(0);
        try {

          reactorTempItemOPC.write(new JIVariant(modellingTemperature.get()));
          reactorTempItemOPC.write(new JIVariant(false));;
        } catch (JIException e) {
          e.printStackTrace();
        }
        return;
      }
      FuzzyLite.logger()
          .info(String.format(
              "growth=%s, closeness=%s, tankOverHeatRisk=%s -> " + actionName
                  + ".output=%s, action=%s",
              Op.str(growthValue), Op.str(closenessValue), Op.str(tankOverheatClosenessValue),
              Op.str(action.getOutputValue()), action.fuzzyOutputValue()));
      notifier(
          String.format(
              "GROWTH=%s,\nCLOSENESS=%s,\nOVERF" + lowLevelName + "_RISK=%s " + "->\n"
                  + " RECOMMENDED "
                  + actionName + "=%s,\nACTIONS=%s",
              tGrowth.highestMembershipTerm(tGrowth.getInputValue()).getName(),
              tCloseness.highestMembershipTerm(closenessValue).getName(),
              tankOverHeatRisk.highestMembershipTerm(tankOverheatClosenessValue).getName(),
              action.highestMembershipTerm(action.getOutputValue()).getName(),
              action.fuzzyOutputValue()),
          tankOverheatClosenessValue);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private static boolean showUserDecisionDialog() {
    // show a joptionpane dialog using showMessageDialog
    Object[] options = {"Turn on coller", "Minus 10 degree from actual temp",
        "Minus 20 degree from actual temp",
        "Stop Heating"};
    int n = JOptionPane.showOptionDialog(closenessChartFrame, "Would you like to stop " +
            "heating?",
        "User decision", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
        options,
        options[2]);
    if (n == 1) {
      modellingTemperature.set(modellingTemperature.get()-10);
      try {

        reactorTempItemOPC.write(new JIVariant(modellingTemperature.get()));
      } catch (JIException e) {
        e.printStackTrace();
      }

    }
    if (n == 2) {
      modellingTemperature.set(modellingTemperature.get()-20);
      try {

        reactorTempItemOPC.write(new JIVariant(modellingTemperature.get()));
      } catch (JIException e) {
        e.printStackTrace();
      }
    }
    if (n == 3) {
      modellingTemperature.set(0);
      try {

        reactorTempItemOPC.write(new JIVariant(modellingTemperature.get()));
        reactorTempItemOPC.write(new JIVariant(false));;
      } catch (JIException e) {
        e.printStackTrace();
      }
      return true;
    }
    if (n == 0) {
      try {

        reactorTempItemOPC.write(new JIVariant(true));
      } catch (JIException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  private static void notifier(String term, double dangerLevel) {

    if (dangerLevel > 0.5d) {
      ImageIcon icon = new ImageIcon(MainPanel.class.getResource("/alarm.png"));
      showErrorNotif(term, new Color(249, 78, 30), icon);
    }
    if (dangerLevel > 0.0d && dangerLevel <= 0.3d) {
      ImageIcon icon = new ImageIcon(MainPanel.class.getResource("/info.png"));
      showErrorNotif(term, new Color(127, 176, 72), icon);
    }
    if (dangerLevel > 0.3d && dangerLevel <= 0.5d) {
      ImageIcon icon = new ImageIcon(MainPanel.class.getResource("/warning.png"));
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
    if (decisionSupportList != null) {
      decisionSupportList.getList().setListData(notifications.toArray());
      decisionSupportList.getList().updateUI();
      decisionSupportList.getFrame().repaint();
    }
    NotificationPopup nf = new NotificationPopup(term);
    nf.setIcon(icon);
    nf.setWIDTH(650);
    nf.setHEIGHT(100);
    nf.setLocation(10, 10);
    nf.setFont(new Font("Tachoma", Font.LAYOUT_LEFT_TO_RIGHT, 12));
    nf.setAlwaysOnTop(true);

    nf.setTitle("Ошибка");
    nf.setDisplayTime(3000);
    nf.setBackgroundColor1(Color.white);
    nf.setBackGroundColor2(color);
    nf.setForegroundColor(java.awt.Color.darkGray);
    nf.display();
    try {
      Thread.sleep(2000);
    } catch (InterruptedException ex) {
      Logger.getLogger(PopupTester.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
}