package br.com.biblioteca.view;

import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.formdev.flatlaf.FlatClientProperties;

/** Lugar provisório para as telas que ainda serão feitas. */
public class EmConstrucaoPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public EmConstrucaoPanel(String nomeTela) {
        setLayout(new GridBagLayout());   // centraliza o conteúdo
        JLabel texto = new JLabel(nomeTela + ": em construção");
        texto.putClientProperty(FlatClientProperties.STYLE_CLASS, "h2");
        texto.setEnabled(false);
        add(texto);
    }
}