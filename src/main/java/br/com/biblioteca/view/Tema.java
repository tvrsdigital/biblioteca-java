package br.com.biblioteca.view;

import java.awt.Color;
import java.util.Map;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

/**
 * Identidade visual do sistema: cores e tema (claro/escuro) com FlatLaf.
 */
public final class Tema {

    // Paleta verde oliva militar
    public static final Color OLIVA = new Color(0x556B2F);
    public static final Color OLIVA_CLARO = new Color(0x8A9A5B);
    public static final Color OLIVA_ESCURO = new Color(0x3B4A21);
    public static final Color OLIVA_MEDIO = new Color(0x6B8E23);

    // Cores de situação (selos e cartões)
    public static final Color VERDE = new Color(0x2E7D32);
    public static final Color LARANJA = new Color(0xE08A00);
    public static final Color AZUL = new Color(0x1E6FB8);
    public static final Color VERMELHO = new Color(0xC62828);

    private static boolean escuro = false;

    private Tema() {
    }

    /** Aplica o tema claro ou escuro e redesenha todas as janelas abertas. */
    public static void aplicar(boolean modoEscuro) {
        escuro = modoEscuro;

        // Valores que a FlatLaf usa ao montar o tema:
        // @accentColor é a cor de destaque (botão principal, seleção, foco)
        FlatLaf.setGlobalExtraDefaults(Map.of(
            "@accentColor", modoEscuro ? "#8A9A5B" : "#556B2F",
            "Table.alternateRowColor", modoEscuro ? "#43463D" : "#F4F6EF"
        ));

        if (modoEscuro) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }

        // Formato dos componentes (vale para os dois temas)
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Table.rowHeight", 28);

        FlatLaf.updateUI();   // atualiza as janelas que já estão abertas
    }

    public static void alternar() {
        aplicar(!escuro);
    }

    public static boolean isEscuro() {
        return escuro;
    }

    /** Fundo dos cartões: muda conforme o tema. */
    public static Color corCartao() {
        return escuro ? new Color(0x46494A) : Color.WHITE;
    }

    /** Borda dos cartões. */
    public static Color corBordaCartao() {
        return escuro ? new Color(0x55595A) : new Color(0xDDE2D3);
    }
}