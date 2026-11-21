package br.com.biblioteca.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.formdev.flatlaf.FlatClientProperties;

/**
 * Cartão de indicador do painel inicial: título, número grande e detalhe,
 * com fundo arredondado e uma faixa colorida à esquerda (desenho próprio).
 */
public class CartaoIndicador extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel valor = new JLabel("–");
    private final JLabel detalhe = new JLabel(" ");
    private Color corDestaque;

    public CartaoIndicador(String titulo, Color corDestaque) {
        this.corDestaque = corDestaque;
        setOpaque(false);   // quem pinta o fundo é o paintComponent
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(18, 26, 18, 18));

        JLabel rotulo = new JLabel(titulo);
        rotulo.putClientProperty(FlatClientProperties.STYLE_CLASS, "medium");
        valor.putClientProperty(FlatClientProperties.STYLE_CLASS, "h00");
        detalhe.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");

        add(rotulo);
        add(valor);
        add(detalhe);
    }

    public void setValor(int numero, String textoDetalhe) {
        valor.setText(String.valueOf(numero));
        detalhe.setText(textoDetalhe);
    }

    public void setCorDestaque(Color cor) {
        this.corDestaque = cor;
        repaint();   // pede ao Swing para redesenhar
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth() - 1;
        int h = getHeight() - 1;

        // Fundo e borda arredondados (cores conforme o tema atual)
        g2.setColor(Tema.corCartao());
        g2.fillRoundRect(0, 0, w, h, 16, 16);
        g2.setColor(Tema.corBordaCartao());
        g2.drawRoundRect(0, 0, w, h, 16, 16);

        // Faixa de destaque à esquerda
        g2.setColor(corDestaque);
        g2.fillRoundRect(0, 0, 8, h + 1, 8, 8);

        g2.dispose();
        super.paintComponent(g);   // depois o Swing desenha os rótulos por cima
    }
}