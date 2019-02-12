package com.nexus.casty;

import com.nexus.casty.player.CastyPlayer;
import com.nexus.casty.server.CastyServer;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.ArrayList;

import static java.awt.GridBagConstraints.*;

public class CastyMain extends JFrame {

	private JLabel statusLabel;
	private JButton toggleButton;
	private JLabel addressLabel;
	private JFormattedTextField portField;
	private JCheckBox showInTrayCheckBox;
	private JTextField hostnameField;

	private TrayIcon trayIcon;

	private enum UIState {
		STOPPED,
		STARTING,
		RUNNING,
		STOPPING
	}

	private void createUIComponents() {
		statusLabel = new JLabel();
		addressLabel = new JLabel();
		toggleButton = new JButton();

		hostnameField = new JTextField(CastyServer.getHostAddress(), 15);

		DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance();
		format.setGroupingUsed(false);
		format.setMaximumFractionDigits(0);

		portField = new JFormattedTextField(format);
		portField.setText(String.valueOf(CastyServer.DEFAULT_PORT));
		portField.setColumns(5);

		showInTrayCheckBox = new JCheckBox();
	}

	private JPanel buildUI() {
		createUIComponents();

		setLayout(new GridBagLayout());
		GridBagConstraints c;

		JPanel serverPanel = new JPanel(new GridBagLayout());
		serverPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Server"), BorderFactory.createEmptyBorder(10, 10, 10, 10)));

		c = new GridBagConstraints(); c.gridx = 0; c.gridy = 0; c.anchor = LINE_END; c.insets.right = 6; c.insets.bottom = 6;
		serverPanel.add(new JLabel("Status"), c);
		c = new GridBagConstraints(); c.gridx = 1; c.gridy = 0; c.anchor = LINE_START; c.weightx = 1; c.insets.bottom = 6;
		serverPanel.add(statusLabel, c);
		c = new GridBagConstraints(); c.gridx = 0; c.gridy = 1; c.anchor = LINE_END; c.insets.right = 6; c.insets.bottom = 6;
		serverPanel.add(new JLabel("Address"), c);
		c = new GridBagConstraints(); c.gridx = 1; c.gridy = 1; c.anchor = LINE_START; c.weightx = 1; c.insets.bottom = 6;
		serverPanel.add(addressLabel, c);
		c = new GridBagConstraints(); c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.anchor = PAGE_START; c.weighty = 1;
		serverPanel.add(toggleButton, c);

		JPanel settingsPanel = new JPanel(new GridBagLayout());
		settingsPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Settings"), BorderFactory.createEmptyBorder(10, 10, 10, 10)));

		c = new GridBagConstraints(); c.gridx = 0; c.gridy = 0; c.anchor = LINE_END; c.insets.right = 6; c.insets.bottom = 6;
		settingsPanel.add(new JLabel("Hostname"), c);
		c = new GridBagConstraints(); c.gridx = 1; c.gridy = 0; c.anchor = LINE_START; c.weightx = 1; c.insets.bottom = 6;
		settingsPanel.add(hostnameField, c);
		c = new GridBagConstraints(); c.gridx = 0; c.gridy = 1; c.anchor = LINE_END; c.insets.right = 6; c.insets.bottom = 6;
		settingsPanel.add(new JLabel("Port"), c);
		c = new GridBagConstraints(); c.gridx = 1; c.gridy = 1; c.anchor = LINE_START; c.weightx = 1; c.insets.bottom = 6;
		settingsPanel.add(portField, c);
		c = new GridBagConstraints(); c.gridx = 0; c.gridy = 2; c.anchor = LINE_END; c.insets.right = 6;
		settingsPanel.add(new JLabel("Show in tray"), c);
		c = new GridBagConstraints(); c.gridx = 1; c.gridy = 2; c.anchor = LINE_START; c.weightx = 1;
		settingsPanel.add(showInTrayCheckBox, c);

		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10 ,10));

		c = new GridBagConstraints(); c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 1; c.fill = BOTH; c.insets.bottom = 6;
		mainPanel.add(serverPanel, c);
		c = new GridBagConstraints(); c.gridx = 0; c.gridy = 1; c.weightx = 1; c.fill = HORIZONTAL;
		mainPanel.add(settingsPanel, c);

		return mainPanel;
	}

	private void addEventListeners()  {
		toggleButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!CastyServer.getInstance().isRunning()) {
					// Get settings
					int port; String hostname;
					try {
						port = Integer.valueOf(portField.getText());
						hostname = hostnameField.getText().isEmpty() ? CastyServer.getHostAddress() : hostnameField.getText();
					}
					catch (NumberFormatException ex) {
						JOptionPane.showMessageDialog(null, "Invalid settings\n" + ex.getMessage(), "Casty", JOptionPane.ERROR_MESSAGE);
						return;
					}

					setUIState(UIState.STARTING);

					new SwingWorker<Void, Void>() {
						@Override
						protected Void doInBackground() throws Exception {
							CastyServer.getInstance().startServer(hostname, port);
							return null;
						}

						@Override
						protected void done() {
							try {
								get();
							} catch (Exception e1) {
								System.err.println("Could not start server");
								e1.printStackTrace();
								JOptionPane.showMessageDialog(null, "Could not start server\nReason: " + e1.getMessage(), "Casty", JOptionPane.ERROR_MESSAGE);
								setUIState(UIState.STOPPED);
								return;
							}
							setUIState(UIState.RUNNING);
						}
					}.execute();

				}
				else {
					setUIState(UIState.STOPPING);
					new SwingWorker<Void, Void>() {
						@Override
						protected Void doInBackground() throws Exception {
							CastyServer.getInstance().stopServer();
							CastyPlayer.getInstance().reset();
							return null;
						}

						@Override
						protected void done() {
							try {
								get();
							} catch (Exception e1) {
								System.err.println("Could not stop server");
								e1.printStackTrace();
								JOptionPane.showMessageDialog(null, "Could not stop server\nReason: " + e1.getMessage(), "Casty", JOptionPane.ERROR_MESSAGE);
								setUIState(UIState.RUNNING);
								return;
							}
							setUIState(UIState.STOPPED);
						}
					}.execute();
				}
			}
		});

		showInTrayCheckBox.addItemListener(e -> {
			if (showInTrayCheckBox.isSelected())
				addToTray();
			else
				removeFromTray();
		});

		addressLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!addressLabel.getText().contains("http://") && !addressLabel.getText().contains("https://"))
					return;
				Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
				if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
					try {
						desktop.browse(new URI(addressLabel.getText().replaceAll("<[^>]+>", "").strip()));
					} catch (Exception ignored) {}
				}
			}
		});

	}

	private CastyMain() {
		super("Casty");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		// Set Icons
		try {
			ArrayList<Image> icons = new ArrayList<>();
			Image icon = ImageIO.read(getClass().getResource("/html/img/app.png"));
			icons.add(icon.getScaledInstance(32, 32, Image.SCALE_SMOOTH));
			icons.add(new ImageIcon(getClass().getResource("/html/img/app-16x16.png")).getImage());
			setIconImages(icons);
		} catch (IOException e) {
			System.err.println("Could not set application icon");
			e.printStackTrace();
		}

		setContentPane(buildUI());
		setUIState(UIState.STOPPED);

		pack();
		setMinimumSize(getSize());
		setLocationRelativeTo(null);

		addEventListeners();

		// Tray Icon
		if (SystemTray.isSupported()) {
			try {
				trayIcon = new TrayIcon(ImageIO.read(getClass().getResource("/html/img/app-16x16.png")));
				trayIcon.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2)
							CastyMain.this.setVisible(true);
					}
				});
			} catch (IllegalArgumentException | IOException e) {
				System.err.println("Could not create tray icon");
				e.printStackTrace();
			}
		}

		setVisible(true);
	}

	private void addToTray() {
		if (trayIcon != null) {
			try {
				SystemTray.getSystemTray().add(trayIcon);
			} catch (AWTException e) {
				System.err.println("Could not add tray icon");
				e.printStackTrace();
				return;
			}
			setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		}
	}

	private void removeFromTray() {
		if (trayIcon != null) {
			SystemTray.getSystemTray().remove(trayIcon);
			setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		}
	}

	private void setUIState(UIState state) {
		switch (state) {
			case STOPPED:
				statusLabel.setText("Stopped");
				addressLabel.setText("None");
				addressLabel.setCursor(null);
				toggleButton.setText("Start server");
				toggleButton.setEnabled(true);
				portField.setEnabled(true);
				hostnameField.setEnabled(true);
				break;
			case STARTING:
				statusLabel.setText("Starting...");
				toggleButton.setEnabled(false);
				portField.setEnabled(false);
				hostnameField.setEnabled(false);
				break;
			case RUNNING:
				statusLabel.setText("Running");
				toggleButton.setText("Stop server");
				addressLabel.setText("<html><font color=blue><u>" + CastyServer.getInstance().getAddress() + "</u></font></html>");
				addressLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				toggleButton.setEnabled(true);
				break;
			case STOPPING:
				statusLabel.setText("Stopping...");
				toggleButton.setEnabled(false);
				break;
		}
	}

	public static void main(String[] args) {
		// Print version
		Package p = CastyMain.class.getPackage();
		if (p != null && p.getImplementationTitle() != null && p.getImplementationVersion() != null) {
			System.out.println(p.getImplementationTitle() + " " +  p.getImplementationVersion());
		}
		else {
			System.out.println("Casty development version");
		}

		// Set look and feel
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.err.println("Could not set look and feel");
			e.printStackTrace();
		}

		// vlcj
		boolean vlc_found = new NativeDiscovery().discover();
		if (!vlc_found) {
			JOptionPane.showMessageDialog(null, "VLC not found. Please make sure VLC is installed on your system.", "Casty", JOptionPane.ERROR_MESSAGE);
			return;
		}
		System.out.println("VLC version: " + LibVlc.INSTANCE.libvlc_get_version());

		// Disable JDK 11 deprecation warnings
		System.setProperty("nashorn.args", "--no-deprecation-warning");

		// Show main form
		new CastyMain();
	}

}
