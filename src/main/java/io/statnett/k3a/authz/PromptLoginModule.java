package io.statnett.k3a.authz;

import org.apache.kafka.common.security.plain.internals.PlainSaslServerProvider;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.spi.LoginModule;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Console;
import java.util.Map;

/**
 * A JAAS LoginModule for Kafka that will look for username and password configuration values,
 * and prompt the user for any value that is missing.
 *
 * If a Java "Console" is available, ie. stdin/stdout is not redirected, the input
 * will be read from the console. Otherwise, this class will try to disable
 * "headless mode", and display a Swing dialog.
 */
public final class PromptLoginModule
implements LoginModule {

    private static final String USERNAME_CONFIG = "username";
    private static final String PASSWORD_CONFIG = "password";
    private Subject subject;
    private String username;
    private String password;

    static {
        System.setProperty("java.awt.headless", "false");
        PlainSaslServerProvider.initialize();
    }

    @Override
    public void initialize(final Subject subject, final CallbackHandler callbackHandler, final Map<String, ?> sharedState, final Map<String, ?> options) {
        this.subject = subject;
        username = (String) options.get(USERNAME_CONFIG);
        password = (String) options.get(PASSWORD_CONFIG);
        promoteNonNulls();
    }

    @Override
    public boolean login() {
        final Console console = System.console();
        if (console != null) {
            inputFromConsole(console);
        } else {
            inputFromWindow();
        }
        promoteNonNulls();
        return true;
    }

    private void inputFromConsole(final Console console) {
        if (username == null) {
            username = console.readLine("Username: ");
        }
        if (password == null) {
            final char[] chars = console.readPassword("Password: ");
            if (chars != null) {
                password = new String(chars);
            }
        }
    }

    private void inputFromWindow() {
        if (username == null || password == null) {
            new UsernamePasswordDialog();
        }
    }

    @Override
    public boolean logout() {
        return true;
    }

    @Override
    public boolean commit() {
        return true;
    }

    @Override
    public boolean abort() {
        return false;
    }

    private void promoteNonNulls() {
        if (username != null) {
            subject.getPublicCredentials().add(username);
        }
        if (password != null) {
            subject.getPrivateCredentials().add(password);
        }
    }

    private final class UsernamePasswordDialog
    extends JDialog {

        UsernamePasswordDialog() {
            super((Frame) null, "JAAS Login", true);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            final JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            final GridBagConstraints constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.insets = new Insets(2, 5, 2, 5);
            final JTextField usernameInput = new JTextField(username == null ? "" : username, 20);
            addInput(panel, constraints, 0, "Username", usernameInput);
            final JPasswordField passwordInput = new JPasswordField(password == null ? "" : password, 20);
            addInput(panel, constraints, 1, "Password", passwordInput);

            final JButton submitButton = new JButton("Submit");
            submitButton.addActionListener(e -> {
                username = usernameInput.getText();
                password = new String(passwordInput.getPassword());
                dispose();
            });
            getRootPane().setDefaultButton(submitButton);
            final JPanel buttonPanel = new JPanel();
            buttonPanel.add(submitButton);

            getContentPane().add(panel, BorderLayout.CENTER);
            getContentPane().add(buttonPanel, BorderLayout.PAGE_END);

            pack();
            setResizable(false);
            setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - getWidth()) / 2,
                        (Toolkit.getDefaultToolkit().getScreenSize().height - getHeight()) / 2);
            addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowGainedFocus(final WindowEvent e) {
                    if (username != null) {
                        passwordInput.requestFocusInWindow();
                    }
                }
            });
            setVisible(true);
        }

        private void addInput(final JPanel panel, final GridBagConstraints constraints, final int y, final String label, final Component input) {
            constraints.gridy = y;
            constraints.gridx = 0;
            constraints.gridwidth = 1;
            panel.add(new JLabel(label), constraints);
            constraints.gridx = 1;
            constraints.gridwidth = 2;
            panel.add(input, constraints);
        }

    }

}
