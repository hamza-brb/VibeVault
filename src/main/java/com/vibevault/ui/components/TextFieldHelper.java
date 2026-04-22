package com.vibevault.ui.components;

import javax.swing.BorderFactory;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public final class TextFieldHelper {
    private TextFieldHelper() {
    }

    public static void styleInputField(JTextField field) {
        field.setBackground(Theme.BG_SURFACE);
        field.setForeground(Theme.TEXT_PRIMARY);
        field.setCaretColor(Theme.ACCENT);
        field.setFont(Theme.body(13f));
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(Theme.BG_BORDER, 16, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
    }

    public static void applyPlaceholder(JTextField field, String placeholder) {
        field.putClientProperty("placeholder", placeholder);
        restorePlaceholderIfEmpty(field);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (isPlaceholderActive(field)) {
                    field.setText("");
                    field.setForeground(Theme.TEXT_PRIMARY);
                    field.putClientProperty("placeholder-active", false);
                    if (field instanceof JPasswordField passwordField) {
                        Character defaultEcho = (Character) passwordField.getClientProperty("default-echo-char");
                        if (defaultEcho != null) {
                            passwordField.setEchoChar(defaultEcho);
                        }
                    }
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                restorePlaceholderIfEmpty(field);
            }
        });
    }

    public static void applyPasswordPlaceholder(JPasswordField field, String placeholder) {
        field.putClientProperty("default-echo-char", field.getEchoChar());
        applyPlaceholder(field, placeholder);
    }

    public static void restorePlaceholderIfEmpty(JTextField field) {
        String text = field.getText();
        if (text != null && !text.isBlank()) {
            return;
        }
        String placeholder = (String) field.getClientProperty("placeholder");
        if (placeholder == null) {
            return;
        }
        field.setText(placeholder);
        field.setForeground(Theme.TEXT_MUTED);
        field.putClientProperty("placeholder-active", true);
        if (field instanceof JPasswordField passwordField) {
            passwordField.setEchoChar((char) 0);
        }
    }

    public static boolean isPlaceholderActive(JTextField field) {
        return Boolean.TRUE.equals(field.getClientProperty("placeholder-active"));
    }

    public static String readTextInput(JTextField field) {
        return isPlaceholderActive(field) ? "" : field.getText();
    }

    public static String readPasswordInput(JPasswordField field) {
        return isPlaceholderActive(field) ? "" : new String(field.getPassword());
    }
}
