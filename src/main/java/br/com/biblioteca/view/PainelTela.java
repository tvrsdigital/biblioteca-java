package br.com.biblioteca.view;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.formdev.flatlaf.FlatClientProperties;

/**
 * Base de todas as telas da barra lateral.
 *
 * CLASSE ABSTRATA: define o contrato "toda tela sabe se atualizar".
 * A MainFrame guarda as telas como PainelTela e chama atualizar() ao exibi-las,
 * sem saber se é a tela de leitores, obras etc. -> POLIMORFISMO.
 */
public abstract class PainelTela extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Recarrega os dados da tela. Chamado sempre que ela é exibida. */
    public abstract void atualizar();

    /** Cabeçalho padrão (título + subtítulo), herdado por todas as telas. */
    protected JPanel criarCabecalho(String titulo, String subtitulo) {
        JPanel cabecalho = new JPanel(new GridLayout(2, 1, 0, 2));
        cabecalho.setOpaque(false);

        JLabel rotuloTitulo = new JLabel(titulo);
        rotuloTitulo.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");
        JLabel rotuloSubtitulo = new JLabel(subtitulo);
        rotuloSubtitulo.putClientProperty(FlatClientProperties.STYLE_CLASS, "medium");

        cabecalho.add(rotuloTitulo);
        cabecalho.add(rotuloSubtitulo);
        return cabecalho;
    }
}