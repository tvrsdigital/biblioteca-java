package br.com.biblioteca.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.formdev.flatlaf.FlatClientProperties;

/**
 * Painel de marca da tela de login: fundo oliva com uma estante de livros
 * desenhada à mão.
 *
 * HERANÇA + SOBRESCRITA na interface: estende JPanel e sobrescreve
 * paintComponent, o método que o Swing chama para desenhar o componente.
 */
public class PainelMarca extends JPanel {

    private static final long serialVersionUID = 1L;

    // Alturas e cores das lombadas (fixas, para o desenho ser sempre igual)
    private static final int[] ALTURAS = {96, 128, 74, 112, 86, 136, 100, 80};
    private static final int[] LARGURAS = {22, 28, 20, 26, 24, 30, 22, 26};
    private static final Color[] CORES = {
        Tema.OLIVA, Tema.OLIVA_CLARO, Tema.OLIVA_MEDIO, new Color(0x7A8450),
        new Color(0xA3A86B), Tema.OLIVA, new Color(0x5E6B3A), Tema.OLIVA_CLARO
    };

    public PainelMarca() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(320, 420));
        setOpaque(true);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setBorder(BorderFactory.createEmptyBorder(44, 32, 0, 32));

        JLabel titulo = new JLabel("Sistema de");
        titulo.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");
        titulo.setForeground(Color.WHITE);

        JLabel titulo2 = new JLabel("Biblioteca");
        titulo2.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");
        titulo2.setForeground(new Color(0xD9E2C4));

        JLabel subtitulo = new JLabel("<html>Leitores, obras, empréstimos<br>e reservas em um só lugar.</html>");
        subtitulo.setForeground(new Color(0xC8D0B4));
        subtitulo.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        textos.add(titulo);
        textos.add(titulo2);
        textos.add(subtitulo);
        add(textos, BorderLayout.NORTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Cópia do Graphics: alterações (cor, antialias) não "vazam" para outros componentes
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fundo
        g2.setColor(Tema.OLIVA_ESCURO);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Lombadas dos livros, lado a lado, apoiadas na "prateleira"
        int base = getHeight() - 40;
        int x = 32;
        for (int i = 0; i < ALTURAS.length; i++) {
            g2.setColor(CORES[i]);
            g2.fillRoundRect(x, base - ALTURAS[i], LARGURAS[i], ALTURAS[i], 6, 6);
            // faixa clara na lombada, como o título de um livro
            g2.setColor(new Color(255, 255, 255, 50));
            g2.fillRect(x + 4, base - ALTURAS[i] + 14, LARGURAS[i] - 8, 4);
            x += LARGURAS[i] + 5;
        }

        // Prateleira
        g2.setColor(new Color(0x2A3418));
        g2.fillRoundRect(20, base, getWidth() - 40, 8, 4, 4);

        g2.dispose();   // libera a cópia
    }
}