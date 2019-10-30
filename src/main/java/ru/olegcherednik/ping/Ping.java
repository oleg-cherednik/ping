package ru.olegcherednik.ping;

import ru.olegcherednik.icoman.IconFile;
import ru.olegcherednik.icoman.IconManager;
import ru.olegcherednik.icoman.exceptions.IconManagerException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * @author Oleg Cherednik
 * @since 30.10.2019
 */
public class Ping implements ActionListener {

    private static final int SC_OK = 200;

    private static final String ID_ON = "on";
    private static final String ID_OFF = "off";

    private static final String CMD_MENU_ABOUT = "about";
    private static final String CMD_MENU_ERROR = "error";
    private static final String CMD_MENU_WARNING = "warning";
    private static final String CMD_MENU_INFO = "info";
    private static final String CMD_MENU_NONE = "none";
    private static final String CMD_MENU_EXIT = "exit";
    private static final String CMD_MAIN = "main";

    private final Image imageOn = createImage(ID_ON);
    private final Image imageOff = createImage(ID_OFF);

    private final TrayIcon trayIcon;
    private final SystemTray tray = SystemTray.getSystemTray();

    public static Ping create() throws IOException, IconManagerException {
        checkSystemTraySupported();
        setLookAndFeel();
        initIconManager();
        return new Ping();
    }

    private static void checkSystemTraySupported() {
        if (!SystemTray.isSupported())
            throw new RuntimeException("SystemTray is not supported");
    }

    private Ping() throws IOException, IconManagerException {
        trayIcon = new TrayIcon(imageOff);
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("util");
        trayIcon.setActionCommand(CMD_MAIN);
    }

    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
//            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch(Exception e) {
            e.printStackTrace();
        }
        /* Turn off metal's use of bold fonts */
        UIManager.put("swing.boldMetal", Boolean.FALSE);
    }

    private static void initIconManager() throws IOException, IconManagerException {
        IconManager iconManager = IconManager.getInstance();

        try (InputStream in = Ping.class.getResourceAsStream("/img/on.ico")) {
            iconManager.addIcon(ID_ON, ImageIO.createImageInputStream(in));
        }

        try (InputStream in = Ping.class.getResourceAsStream("/img/off.ico")) {
            iconManager.addIcon(ID_OFF, ImageIO.createImageInputStream(in));
        }
    }

    private void setVisible() throws IOException, IconManagerException, AWTException {
        new TaskDaemon().start();
        trayIcon.setPopupMenu(createPopupMenu());
        trayIcon.addActionListener(this);
        tray.add(trayIcon);
    }

    private PopupMenu createPopupMenu() {
        PopupMenu popup = new PopupMenu();
        popup.add(createMenuItem("About", CMD_MENU_ABOUT));
        popup.addSeparator();
        popup.add(createMenuDisplay());
        popup.add(createMenuItem("Exit", CMD_MENU_EXIT));
        return popup;
    }

    private Menu createMenuDisplay() {
        Menu menu = new Menu("Display");
        menu.add(createMenuItem("Error", CMD_MENU_ERROR));
        menu.add(createMenuItem("Warning", CMD_MENU_WARNING));
        menu.add(createMenuItem("Info", CMD_MENU_INFO));
        menu.add(createMenuItem("None", CMD_MENU_NONE));
        return menu;
    }

    private MenuItem createMenuItem(String title, String command) {
        MenuItem menuItem = new MenuItem(title);
        menuItem.setActionCommand(command);
        menuItem.addActionListener(this);
        return menuItem;
    }

    private static Image createImage(String id) throws IconManagerException, IOException {
        IconFile iconFile = IconManager.getInstance().getIconFile(id);
        return new ImageIcon(iconFile.getImage(iconFile.getIds().iterator().next())).getImage();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (CMD_MENU_ABOUT.equals(command))
            JOptionPane.showMessageDialog(null, "This dialog box is run from the About menu item");
        else if (CMD_MENU_ERROR.equals(command))
            trayIcon.displayMessage("Sun TrayIcon Demo", "This is an error message", TrayIcon.MessageType.ERROR);
        else if (CMD_MENU_WARNING.equals(command))
            trayIcon.displayMessage("Sun TrayIcon Demo", "This is a warning message", TrayIcon.MessageType.WARNING);
        else if (CMD_MENU_INFO.equals(command))
            trayIcon.displayMessage("Sun TrayIcon Demo", "This is an info message", TrayIcon.MessageType.INFO);
        else if (CMD_MENU_NONE.equals(command))
            trayIcon.displayMessage("Sun TrayIcon Demo", "This is an ordinary message", TrayIcon.MessageType.NONE);
        else if (CMD_MENU_EXIT.equals(command)) {
            tray.remove(trayIcon);
            System.exit(0);
        } else if (CMD_MAIN.equals(command))
            JOptionPane.showMessageDialog(null, "This dialog box is run from System Tray");

    }

    private final class TaskDaemon extends Thread {

        private final long interval = TimeUnit.SECONDS.toMillis(5);
        private final int connectTimeout = (int)TimeUnit.SECONDS.toMillis(5);
        private final int readTimeout = (int)TimeUnit.SECONDS.toMillis(5);

        public TaskDaemon() {
            super("ping-task-daemon");
            setDaemon(true);
        }

        private void checkStatus() {
            try {
//                    URL url = new URL("http://trialingutil.exp-design-stage.apps.usae-2.syngentaaws.org/health");
                URL url = new URL("http://localhost:8081/health");
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(connectTimeout);
                con.setReadTimeout(readTimeout);

                int status = con.getResponseCode();
                trayIcon.setImage(status == SC_OK ? imageOn : imageOff);
            } catch(Exception e) {
                trayIcon.setImage(imageOff);
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    checkStatus();
                    Thread.sleep(interval);
                } catch(InterruptedException e) {
                    return;
                }
            }
        }
    }

    public static void main(String... args) {
        SwingUtilities.invokeLater(() -> {
            try {
                create().setVisible();
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
    }
}
