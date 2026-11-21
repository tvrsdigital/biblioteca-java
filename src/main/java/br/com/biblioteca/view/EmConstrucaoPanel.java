package br.com.biblioteca.view;

import java.awt.GridBagLayout;

import javax.swing.JLabel;

import com.formdev.flatlaf.FlatClientProperties;

/** Lugar provisório para as telas que ainda serão feitas. */
public class EmConstrucaoPanel extends PainelTela {

    private static final long serialVersionUID = 1L;

    public EmConstrucaoPanel(String nomeTela) {
        setLayout(new GridBagLayout());
        JLabel texto = new JLabel(nomeTela + ": em construção");
        texto.putClientProperty(FlatClientProperties.STYLE_CLASS, "h2");
        texto.setEnabled(false);
        add(texto);
    }

    @Override
    public void atualizar() {
        // nada a atualizar
    }
}