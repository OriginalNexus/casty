package com.nexus.casty;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.text.DecimalFormat;

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

		DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance();
		format.setGroupingUsed(false);
		portField = new JFormattedTextField(format);
		portField.setText("5555");
		portField.setColumns(5);
		hostnameField = new JTextField("192.168.0.13", 15);
		showInTrayCheckBox = new JCheckBox();
	}

	private JPanel buildUI() {
		createUIComponents();

		JPanel serverPanel = FormBuilder.create()
				.columns("r:d, 4dlu, f:d:g")
				.rows("d, 4dlu, d, 4dlu, d")
				.border(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Server"), Paddings.DLU7))
				.add("Status").xy(1, 1)
				.add(statusLabel).xy(3, 1)
				.add("Address").xy(1, 3)
				.add(addressLabel).xy(3, 3)
				.add(toggleButton).xyw(1, 5, 3, "center, center").build();

		JPanel settingsPanel = FormBuilder.create()
				.columns("r:d, 4dlu, l:d")
				.rows("d, 4dlu, d, 4dlu, d")
				.border(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Settings"), Paddings.DLU7))
				.add("Port").xy(1, 1)
				.add(portField).xy(3, 1)
				.add("Hostname").xy(1, 3)
				.add(hostnameField).xy(3, 3)
				.add("Show in tray").xy(1, 5)
				.add(showInTrayCheckBox).xy(3, 5)
				.build();

		return FormBuilder.create()
				.columns("f:d:g")
				.rows("f:d:g, 4dlu, f:d")
				.border(Paddings.DIALOG)
				.add(serverPanel).xy(1, 1)
				.add(settingsPanel).xy(1, 3)
				.build();
	}

	private void addEventListeners() {
		toggleButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!CastyServer.getInstance().isRunning()) {
					// Get port and hostname
					int port;
					String hostname;
					try {
						port = Integer.valueOf(portField.getText());
					} catch (IllegalArgumentException ex) {
						JOptionPane.showMessageDialog(null, "Invalid port", "Casty", JOptionPane.ERROR_MESSAGE);
						return;
					}
					hostname = (hostnameField.getText().isEmpty() ? "localhost" : hostnameField.getText());

					setUIState(UIState.STARTING);

					(new SwingWorker<Void, Void>() {
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
								JOptionPane.showMessageDialog(null, "Could not start server\nReason: " + e1.getMessage(), "Casty", JOptionPane.ERROR_MESSAGE);
								setUIState(UIState.STOPPED);
								return;
							}
							setUIState(UIState.RUNNING);
						}
					}).execute();

				}
				else {
					setUIState(UIState.STOPPING);
					(new SwingWorker<Void, Void>() {
						@Override
						protected Void doInBackground() throws Exception {
							CastyServer.getInstance().stopServer();
							return null;
						}

						@Override
						protected void done() {
							setUIState(UIState.STOPPED);
						}
					}).execute();
				}
			}
		});

		showInTrayCheckBox.addItemListener(e -> {
			if (showInTrayCheckBox.isSelected()) addToTray();
			else removeFromTray();
		});

		addressLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!addressLabel.getText().startsWith("http://") && !addressLabel.getText().startsWith("https://"))
					return;
				Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
				if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
					try {
						desktop.browse(new URI(addressLabel.getText()));
					} catch (Exception ignored) {}
				}
			}
		});
	}


	private CastyMain() {
		super("Casty");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		setContentPane(buildUI());
		setUIState(UIState.STOPPED);

		pack();
		setMinimumSize(getSize());
		setSize(300, 300);
		setLocationRelativeTo(null);

		addEventListeners();

		// Tray Icon
		if (SystemTray.isSupported()) {
			try {
				trayIcon = new TrayIcon(ImageIO.read(getClass().getResource("/icon.png")), "Casty");
				trayIcon.setImageAutoSize(true);
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
				toggleButton.setText("Start server");
				toggleButton.setEnabled(true);
				portField.setEnabled(true);
				hostnameField.setEnabled(true);
				break;
			case STARTING:
				statusLabel.setText("Starting...");
				addressLabel.setText("None");
				toggleButton.setText("Start server");
				toggleButton.setEnabled(false);
				portField.setEnabled(false);
				hostnameField.setEnabled(false);
				break;
			case RUNNING:
				statusLabel.setText("Running");
				toggleButton.setText("Stop server");
				addressLabel.setText(CastyServer.getInstance().getAddress());
				toggleButton.setEnabled(true);
				portField.setEnabled(false);
				hostnameField.setEnabled(false);
				break;
			case STOPPING:
				statusLabel.setText("Stopping...");
				toggleButton.setText("Stop server");
				toggleButton.setEnabled(false);
				portField.setEnabled(false);
				hostnameField.setEnabled(false);
				break;
		}
	}

	public static void main(String[] args) {
		System.out.println("Casty 1.0.0");

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

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new CastyMain();
			frame.setVisible(true);
		});

	}

}
