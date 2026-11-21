package br.com.biblioteca.view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

/** Painel com fundo e borda arredondados, nas cores do tema atual. */
public class PainelCartao extends JPanel {

    private static final long serialVersionUID = 1L;

    public PainelCartao() {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Tema.corCartao());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        g2.setColor(Tema.corBordaCartao());
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        g2.dispose();
        super.paintComponent(g);
    }
}