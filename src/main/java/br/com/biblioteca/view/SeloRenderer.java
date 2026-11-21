package br.com.biblioteca.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Desenha o valor da célula como um "selo" colorido (pílula arredondada).
 * Recebe um mapa texto -> cor, então serve para qualquer tabela:
 * tipos de leitor, situação de cópias, situação de reservas...
 *
 * SOBRESCRITA: o Swing chama getTableCellRendererComponent para cada célula
 * e paintComponent para desenhá-la; nós substituímos os dois.
 */
public class SeloRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    private final Map<String, Color> cores;
    private Color corAtual = Color.GRAY;

    public SeloRenderer(Map<String, Color> cores) {
        this.cores = cores;
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionada,
                                                   boolean focada, int linha, int coluna) {
        // O super cuida do fundo (seleção e linhas alternadas)
        super.getTableCellRendererComponent(tabela, valor, selecionada, false, linha, coluna);
        String texto = (valor == null) ? "" : valor.toString();
        setText(texto);
        corAtual = cores.getOrDefault(texto, Color.GRAY);   // cor do mapa, ou cinza
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fundo da célula
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (!getText().isEmpty()) {
            Font fonte = getFont().deriveFont(Font.BOLD, getFont().getSize2D() - 1f);
            g2.setFont(fonte);
            FontMetrics fm = g2.getFontMetrics();

            int largura = fm.stringWidth(getText()) + 22;
            int altura = fm.getHeight() + 4;
            int x = (getWidth() - largura) / 2;
            int y = (getHeight() - altura) / 2;

            // Pílula: a mesma cor, transparente no fundo e forte no texto
            g2.setColor(new Color(corAtual.getRed(), corAtual.getGreen(), corAtual.getBlue(), 45));
            g2.fillRoundRect(x, y, largura, altura, altura, altura);
            g2.setColor(corAtual);
            g2.drawString(getText(), x + 11, y + 2 + fm.getAscent());
        }
        g2.dispose();
    }
}