/**
 * rscminus
 *
 * This file is part of rscminus.
 *
 * rscminus is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * rscminus is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with rscminus. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * Authors: see <https://github.com/OrN/rscminus>
 */

package rscminus.scraper;

import rscminus.common.FileUtil;
import rscminus.common.Logger;
import rscminus.common.Settings;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

/**
 * GUI designed for the RSCPlus client that manages Stripperuration options and keybind values from
 * within an interface.
 *
 * <p><b>To add a new Stripperuration option to the GUI,</b> <br>
 * 1.) Declare an instance variable to hold the gui element (eg checkbox) and add it to the GUI from
 * StripperWindow.initialize() (see existing examples) <br>
 * 1.5.) If there is a helper method such as addCheckbox, use that method to create and store the
 * element that is returned in the StripperWindow.initialize() method. See existing code for examples.
 * <br>
 * 2.) ^Add an appropriate variable to the Settings class as a class variable, <i>and</i> as an
 * assignment in the appropriate restore default method below. <br>
 * 3.) Add an entry in the StripperWindow.synchronizeGuiValues() method that references the variable,
 * as per the already-existing examples.<br>
 * 4.) Add an entry in the StripperWindow.saveSettings() method referencing the variable, as per the
 * already-existing examples.<br>
 * 5.) ^Add an entry in the Settings.Save() class save method to save the option to file.<br>
 * 6.) ^Add an entry in the Settings.Load() class load method to load the option from file.<br>
 * 7.) (Optional) If a method needs to be called to adjust settings other than the setting value
 * itself, add it to the StripperWindow.applySettings() method below.<br>
 * <br>
 * <i>Entries marked with a ^ are steps used to add settings that are not included in the GUI.</i>
 * <br>
 * <br>
 * <b>To add a new keybind,</b><br>
 * 1.) Add a call in the initialize method to addKeybind with appropriate parameters.<br>
 * 2.) Add an entry to the command switch statement in Settings to process the command when its
 * keybind is pressed.<br>
 * 3.) Optional, recommended: Separate the command from its functionality by making a toggleBlah
 * method and calling it from the switch statement.<br>
 */
public class StripperWindow {

  private JFrame frame;

  JTabbedPane tabbedPane;

  //// General tab


  //// Overlays tab


  //// Notifications tab


  //// Replay tab


  //// Presets tab


  public StripperWindow() {
    try {
      // Set System L&F as a fall-back option.
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          NimbusLookAndFeel laf = (NimbusLookAndFeel) UIManager.getLookAndFeel();
          laf.getDefaults().put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 11));
          break;
        }
      }
    } catch (UnsupportedLookAndFeelException e) {
      Logger.Error("Unable to set L&F: Unsupported look and feel");
    } catch (ClassNotFoundException e) {
      Logger.Error("Unable to set L&F: Class not found");
    } catch (InstantiationException e) {
      Logger.Error("Unable to set L&F: Class object cannot be instantiated");
    } catch (IllegalAccessException e) {
      Logger.Error("Unable to set L&F: Illegal access exception");
    }
    initialize();
  }

  public void showStripperWindow() {
    frame.setVisible(true);
  }

  public void hideStripperWindow() {
    frame.setVisible(false);
  }

  /** Initialize the contents of the frame. */
  private void initialize() {
    Logger.Info("Creating Stripper Window");
    try {
      SwingUtilities.invokeAndWait(
              new Runnable() {

                @Override
                public void run() {
                  runInit();
                }
              });
    } catch (InvocationTargetException e) {
      Logger.Error("There was a thread-related error while setting up the Stripper window!");
      e.printStackTrace();
    } catch (InterruptedException e) {
      Logger.Error(
              "There was a thread-related error while setting up the Stripper window! The window may not be initialized properly!");
      e.printStackTrace();
    }
  }

  private void runInit() {
    frame = new JFrame();
    frame.setTitle("RSCMinus");
    frame.setBounds(100, 100, 800, 650);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.getContentPane().setLayout(new BorderLayout(0, 0));
    URL iconURL = Settings.getResource("/assets/rscminus-logo.png");
    if (iconURL != null) {
      ImageIcon icon = new ImageIcon(iconURL);
      frame.setIconImage(icon.getImage());
    }

    tabbedPane = new JTabbedPane();

    JScrollPane aboutScrollPane = new JScrollPane();
    JScrollPane scrapeScrollPane = new JScrollPane();
    JScrollPane stripScrollPane = new JScrollPane();
    JScrollPane donateScrollPane = new JScrollPane();

    JPanel aboutPanel = new JPanel();
    JPanel scrapePanel = new JPanel();
    JPanel stripPanel = new JPanel();
    JPanel donatePanel = new JPanel();


    frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
    tabbedPane.setFont(new Font("", Font.PLAIN,16));
    tabbedPane.addTab("About", null, aboutScrollPane, null);
    tabbedPane.addTab("Scrape", null, scrapeScrollPane, null);
    tabbedPane.addTab("Strip", null, stripScrollPane, null);

    // TODO: ornox uncomment this, maybe add some way to donate in the gui
    // TODO: possibly user configurable in settings if this tab gets added or not, on by default
    //tabbedPane.addTab("Contribute ♥", null, donateScrollPane, null);


    aboutScrollPane.setViewportView(aboutPanel);
    scrapeScrollPane.setViewportView(scrapePanel);
    stripScrollPane.setViewportView(stripPanel);
    donateScrollPane.setViewportView(donatePanel);

    // Adding padding for aesthetics
    aboutPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    scrapePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    stripPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    donatePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    setScrollSpeed(aboutScrollPane,  20, 15);
    setScrollSpeed(scrapeScrollPane, 20, 15);
    setScrollSpeed(stripScrollPane,  20, 15);
    setScrollSpeed(donateScrollPane, 20, 15);


    /*
     * About tab
     */
    aboutPanel.setLayout(new BoxLayout(aboutPanel, BoxLayout.Y_AXIS));

    JPanel thirdsPanel= new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridy = 0;
    c.gridx = 0;
    c.fill = GridBagConstraints.HORIZONTAL;

    JPanel logoPanel = new JPanel();
    try {
      BufferedImage rscminusLogo = ImageIO.read(new File(FileUtil.findDirectoryReverse("/assets") + "/assets/rscminus-logo.png"));
      JLabel rscminusLogoJLabel = new JLabel(new ImageIcon(rscminusLogo.getScaledInstance(256, 256, Image.SCALE_SMOOTH)));
      rscminusLogoJLabel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
      logoPanel.add(rscminusLogoJLabel);
    } catch(Exception e) {
      e.printStackTrace();
    }

    thirdsPanel.add(logoPanel,c);

    JPanel rightPane = new JPanel(new GridBagLayout());
    GridBagConstraints cR = new GridBagConstraints();
    cR.fill = GridBagConstraints.VERTICAL;
    cR.anchor = GridBagConstraints.LINE_START;
    cR.weightx = 0.5;
    cR.gridy = 0;
    cR.gridwidth = 3;

    JLabel RSCMinusText = new JLabel("<html><div style=\"font-size:45px; padding-bottom:10px;\"<b>RSC</b>Minus</div><div style=\"font-size:20px;\">Gui Edition v" + Settings.versionNumber + "</div></html>");

    rightPane.add(RSCMinusText);

    cR.gridy = 1;

    JLabel aboutText = new JLabel("<html><head><style>p{font-size:10px; padding-top:15px;}</style></head><p><b>RSC</b>Minus is a 235-protocol compatible \"Proof of Concept\"<br/>"+
            "server core. It also has the ability to very quickly process<br/>"+
            "replays to automatically scrape data, or to strip chat<br/>"+
            "out of them & optimize their compression.</p>"+
            "<p>Use the tabs at the top to pick a function.</p></html>");


    rightPane.add(aboutText, cR);
    c.gridx = 2;
    thirdsPanel.add(rightPane,c);

    JPanel bottomPane = new JPanel(new GridBagLayout());
    GridBagConstraints cB = new GridBagConstraints();
    cB = new GridBagConstraints();
    cB.fill = GridBagConstraints.HORIZONTAL;
    cB.anchor = GridBagConstraints.NORTH;

    cB.gridx = 0;
    cB.weightx = 0.33;
    cB.gridwidth = 1;

    JLabel licenseText = new JLabel("Licensed under GPLv3");
    bottomPane.add(licenseText,cB);

    cB.gridx = 5;
    cB.weightx = 1;
    cB.gridwidth = 20;
    JLabel blank = new JLabel("");
    bottomPane.add(blank,cB);

    cB.gridx = 30;
    cB.weightx = 0.33;
    cB.gridwidth = 1;
    JLabel authorsLink = new JLabel("Authors: Ornox & Logg");
    authorsLink.setBorder(BorderFactory.createEmptyBorder(0,400,0,0)); //don't ask
    bottomPane.add(authorsLink,cB);

    c.gridy = 10;
    c.gridx = 0;
    c.gridwidth = 10;
    thirdsPanel.add(bottomPane, c);

    aboutPanel.add(thirdsPanel);


    /*
     * Scrape tab
     */

    scrapePanel.setLayout(new BoxLayout(scrapePanel, BoxLayout.Y_AXIS));
    scrapePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    addSettingsHeader(scrapePanel, "Get stuff out of replays");

    JPanel scrapePanelCheckboxesPanel = new JPanel();
    scrapePanelCheckboxesPanel.setLayout(new BoxLayout(scrapePanelCheckboxesPanel,BoxLayout.Y_AXIS));
    JCheckBox dumpObjectsCheckbox =  addCheckbox("Dump Objects",scrapePanelCheckboxesPanel);
    dumpObjectsCheckbox.setSelected(Settings.dumpObjects);
    dumpObjectsCheckbox.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                Settings.dumpObjects = !Settings.dumpObjects;
              }
            }
    );

    JCheckBox dumpWallObjectsCheckbox =  addCheckbox("Dump Wallobjects",scrapePanelCheckboxesPanel);
    dumpWallObjectsCheckbox.setSelected(Settings.dumpWallObjects);
    dumpWallObjectsCheckbox.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                Settings.dumpWallObjects = !Settings.dumpWallObjects;
              }
            }
    );

    JLabel explainWhatTheTextBoxIsFor = new JLabel("Paste the top directory for the replay(s) you want to scrape:");
    scrapePanelCheckboxesPanel.add(explainWhatTheTextBoxIsFor);

    JPanel aJPanelForOnlyOneComponentFuckJPanel = new JPanel();
    aJPanelForOnlyOneComponentFuckJPanel.setLayout(new BoxLayout(aJPanelForOnlyOneComponentFuckJPanel, BoxLayout.Y_AXIS));
    aJPanelForOnlyOneComponentFuckJPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    JTextField replayDirectoryTextField = new JTextField();
    replayDirectoryTextField.setMinimumSize(new Dimension(100, 28));
    replayDirectoryTextField.setMaximumSize(new Dimension(500, 28));
    replayDirectoryTextField.setAlignmentY((float) 0.75);
    aJPanelForOnlyOneComponentFuckJPanel.add(replayDirectoryTextField);
    scrapePanelCheckboxesPanel.add(aJPanelForOnlyOneComponentFuckJPanel);

    addButton("Start",scrapePanelCheckboxesPanel,Component.LEFT_ALIGNMENT)
            .addActionListener(
                new ActionListener() {
                  @Override
                  public void actionPerformed(ActionEvent e) {
                    Settings.sanitizePath = replayDirectoryTextField.getText();
                    Settings.sanitizeOutputPath = new File(Settings.sanitizePath, "../output").toString();
                    if (replayDirectoryTextField.getText().length() > 0) {
                      if (!Scraper.stripping) {
                        Logger.Info("Scraping " + Settings.sanitizePath);
                        Scraper.scrape();
                      } else {
                        Logger.Warn("Already scraping, please wait.");
                      }
                    }
                  }
                }
            );

    scrapePanel.add(scrapePanelCheckboxesPanel);

    //TODO put "start" button inline, to the right of the text field

    //TODO add jlabel "OR" here
    //TODO add file chooser here

    /*
     * Strip tab
     */
    stripPanel.setLayout(new BoxLayout(stripPanel, BoxLayout.Y_AXIS));
    addSettingsHeader(stripPanel, "Strip Chat or Just Optimize Replay Data (recompress)");
    stripPanel.setLayout(new BoxLayout(stripPanel, BoxLayout.Y_AXIS));
    stripPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    JPanel stripPanelCheckboxesPanel = new JPanel();
    stripPanelCheckboxesPanel.setLayout(new BoxLayout(stripPanelCheckboxesPanel,BoxLayout.Y_AXIS));

    JCheckBox stripPublicChatCheckbox =  addCheckbox("Delete Public Chat (makes replays depressing, only use if necessary)",stripPanelCheckboxesPanel);
    stripPublicChatCheckbox.setSelected(Settings.sanitizePublicChat);
    stripPublicChatCheckbox.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                Settings.sanitizePublicChat = !Settings.sanitizePublicChat;
              }
            }
    );

    JCheckBox stripPrivateChatCheckbox =  addCheckbox("Delete Private Messages",stripPanelCheckboxesPanel);
    stripPrivateChatCheckbox.setSelected(Settings.sanitizePrivateChat);
    stripPrivateChatCheckbox.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                Settings.sanitizePrivateChat = !Settings.sanitizePrivateChat;
              }
            }
    );

    JCheckBox stripPrivateFriendsUpdateCheckbox =  addCheckbox("Delete Friends/Ignore Lists and Log In/Out Messages",stripPanelCheckboxesPanel);
    stripPrivateFriendsUpdateCheckbox.setSelected(Settings.sanitizeFriendsIgnore);
    stripPrivateFriendsUpdateCheckbox.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                Settings.sanitizeFriendsIgnore = !Settings.sanitizeFriendsIgnore;
              }
            }
    );

    explainWhatTheTextBoxIsFor = new JLabel("Paste the top directory for the replay(s) you want to optimize or strip data from:");
    stripPanelCheckboxesPanel.add(explainWhatTheTextBoxIsFor);

    JPanel aJPanelForOnlyOneComponentFuckJPanel2 = new JPanel();
    aJPanelForOnlyOneComponentFuckJPanel2.setLayout(new BoxLayout(aJPanelForOnlyOneComponentFuckJPanel2, BoxLayout.Y_AXIS));
    aJPanelForOnlyOneComponentFuckJPanel2.setAlignmentX(Component.LEFT_ALIGNMENT);
    JTextField replayDirectoryTextField2 = new JTextField();
    replayDirectoryTextField2.setMinimumSize(new Dimension(100, 28));
    replayDirectoryTextField2.setMaximumSize(new Dimension(500, 28));
    replayDirectoryTextField2.setAlignmentY((float) 0.75);
    aJPanelForOnlyOneComponentFuckJPanel2.add(replayDirectoryTextField2);
    stripPanelCheckboxesPanel.add(aJPanelForOnlyOneComponentFuckJPanel2);

    addButton("Start",stripPanelCheckboxesPanel,Component.LEFT_ALIGNMENT)
            .addActionListener(
                    new ActionListener() {
                      @Override
                      public void actionPerformed(ActionEvent e) {
                        Settings.sanitizePath = replayDirectoryTextField2.getText();
                        Settings.sanitizeOutputPath = new File(Settings.sanitizePath,"../output").toString();
                        if (replayDirectoryTextField2.getText().length() > 0) {
                          if (!Scraper.stripping) {
                            Logger.Info("Stripping/Optimizing " + Settings.sanitizePath);
                            Scraper.strip();
                          } else {
                            Logger.Warn("Already stripping/optimizing, please wait.");
                          }
                        }
                      }
                    }
            );

    stripPanel.add(stripPanelCheckboxesPanel);

    //TODO put "start" button inline, to the right of the text field

    //TODO add jlabel "OR" here
    //TODO add file chooser here



    /*
     * Donate tab
     */
    donatePanel.setLayout(new BoxLayout(donatePanel, BoxLayout.Y_AXIS));
    addSettingsHeader(donatePanel, "How to donate"); //pm ornox ;)


  }


  /**
   * Adds a new category title to the notifications list.
   *
   * @param panel Panel to add the title to.
   * @param categoryName Name of the category to add.
   */
  private void addSettingsHeader(JPanel panel, String categoryName) {
    addSettingsHeaderLabel(panel, "<html><div style=\"font-size:10px;\"><b>" + categoryName + "</b></div></html>");
    addSettingsHeaderSeparator(panel);
  }

  /**
   * Adds a new horizontal separator to the notifications list.
   *
   * @param panel Panel to add the separator to.
   */
  private void addSettingsHeaderSeparator(JPanel panel) {
    JSeparator jsep = new JSeparator(SwingConstants.HORIZONTAL);
    jsep.setMaximumSize(new Dimension(Short.MAX_VALUE, 7));
    panel.add(jsep);
  }

  /**
   * Adds a new category label to the notifications list.
   *
   * @param panel Panel to add the label to.
   * @param categoryName Name of the category to add.
   * @return The label that was added.
   */
  private JLabel addSettingsHeaderLabel(JPanel panel, String categoryName) {
    JLabel jlbl = new JLabel(categoryName);
    jlbl.setHorizontalAlignment(SwingConstants.LEFT);
    panel.add(jlbl);
    return jlbl;
  }

  /**
   * Adds a preStripperured JCheckbox to the specified container, setting its alignment constraint to
   * left and adding an empty padding border.
   *
   * @param text The text of the checkbox
   * @param container The container to add the checkbox to.
   * @return The newly created JCheckBox.
   */
  private JCheckBox addCheckbox(String text, Container container) {
    JCheckBox checkbox = new JCheckBox(text);
    checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);
    checkbox.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 5));
    container.add(checkbox);
    return checkbox;
  }

  /**
   * Adds a preStripperured JButton to the specified container using the specified alignment
   * constraint. Does not modify the button's border.
   *
   * @param text The text of the button
   * @param container The container to add the button to
   * @param alignment The alignment of the button.
   * @return The newly created JButton.
   */
  private JButton addButton(String text, Container container, float alignment) {
    JButton button = new JButton(text);
    button.setAlignmentX(alignment);
    container.add(button);
    return button;
  }

  /**
   * Adds a preStripperured radio button to the specified container. Does not currently assign the
   * radio button to a group.
   *
   * @param text The text of the radio button
   * @param container The container to add the button to
   * @param leftIndent The amount of padding to add to the left of the radio button as an empty
   *     border argument.
   * @return The newly created JRadioButton
   */
  private JRadioButton addRadioButton(String text, Container container, int leftIndent) {
    JRadioButton radioButton = new JRadioButton(text);
    radioButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    radioButton.setBorder(BorderFactory.createEmptyBorder(0, leftIndent, 7, 5));
    container.add(radioButton);
    return radioButton;
  }

  /**
   * Sets the scroll speed of a JScrollPane
   *
   * @param scrollPane The JScrollPane to modify
   * @param horizontalInc The horizontal increment value
   * @param verticalInc The vertical increment value
   */
  private void setScrollSpeed(JScrollPane scrollPane, int horizontalInc, int verticalInc) {
    scrollPane.getVerticalScrollBar().setUnitIncrement(verticalInc);
    scrollPane.getHorizontalScrollBar().setUnitIncrement(horizontalInc);
  }
  public void disposeJFrame() {
    frame.dispose();
  }

  //TODO implement
  /*
  public static boolean replayFileChooser() {
    JFileChooser j;
    try {
      //j = new JFileChooser(Settings.REPLAY_BASE_PATH.get("custom"));
    } catch (Exception e) {
      j = new JFileChooser(Settings.Dir.REPLAY);
    }
    j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    //int response = j.showDialog(,"Select Folder");

    File selection = j.getSelectedFile();

    return false;
  }
  */

}
