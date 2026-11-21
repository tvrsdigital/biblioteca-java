package br.com.biblioteca.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import com.formdev.flatlaf.FlatClientProperties;

import br.com.biblioteca.model.Funcionario;

/**
 * Janela principal: barra lateral de navegação + área de conteúdo (CardLayout).
 * As telas ficam num Map<String, PainelTela>: ao exibir uma, chamamos atualizar()
 * sem saber qual tela é -> POLIMORFISMO.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String ESTILO_ITEM_MENU =
        "arc: 10; borderWidth: 0; focusWidth: 0;"
        + "background: #3B4A21; foreground: #E3E8D6;"
        + "hoverBackground: #4A5C2A; pressedBackground: #55693A;"
        + "selectedBackground: #6B8E23; selectedForeground: #FFFFFF";

    private final Funcionario funcionarioLogado;   // RN09: quem registra os empréstimos
    private final CardLayout cartoes = new CardLayout();
    private final JPanel conteudo = new JPanel(cartoes);
    private final Map<String, PainelTela> telas = new HashMap<>();
    private final ButtonGroup grupoMenu = new ButtonGroup();
    private JButton botaoTema;

    public MainFrame(Funcionario funcionarioLogado) {
        super("Sistema de Biblioteca");
        this.funcionarioLogado = funcionarioLogado;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 700));

        registrarTela("inicio", new InicioPanel(funcionarioLogado));
        registrarTela("leitores", new LeitoresPanel());
        registrarTela("obras", new ObrasPanel());
        registrarTela("emprestimos", new EmprestimosPanel(funcionarioLogado));
        registrarTela("reservas", new ReservasPanel());
        
        setLayout(new BorderLayout());
        add(montarBarraLateral(), BorderLayout.WEST);
        add(conteudo, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        mostrar("inicio");
    }

    public Funcionario getFuncionarioLogado() {
        return funcionarioLogado;
    }

    private void registrarTela(String nome, PainelTela tela) {
        telas.put(nome, tela);
        conteudo.add(tela, nome);
    }

    /** Exibe uma tela pelo nome, atualizando seus dados antes. */
    private void mostrar(String nome) {
        telas.get(nome).atualizar();   // POLIMORFISMO: cada tela atualiza do seu jeito
        cartoes.show(conteudo, nome);
    }

    private JPanel montarBarraLateral() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Tema.OLIVA_ESCURO);
        barra.setPreferredSize(new Dimension(240, 0));
        barra.setBorder(BorderFactory.createEmptyBorder(24, 16, 20, 16));

        // --- Topo: nome do sistema ---
        JPanel topo = new JPanel();
        topo.setOpaque(false);
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        JLabel nome1 = new JLabel("Sistema de");
        nome1.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3");
        nome1.setForeground(new Color(0xC8D0B4));
        JLabel nome2 = new JLabel("Biblioteca");
        nome2.putClientProperty(FlatClientProperties.STYLE_CLASS, "h2");
        nome2.setForeground(Color.WHITE);
        topo.add(nome1);
        topo.add(nome2);
        topo.add(Box.createVerticalStrut(28));

        // --- Meio: itens de navegação ---
        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        JToggleButton inicio = criarItemMenu("Início", "inicio");
        menu.add(inicio);
        menu.add(criarItemMenu("Leitores", "leitores"));
        menu.add(criarItemMenu("Obras e cópias", "obras"));
        menu.add(criarItemMenu("Empréstimos", "emprestimos"));
        menu.add(criarItemMenu("Reservas", "reservas"));
        inicio.setSelected(true);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(topo, BorderLayout.NORTH);
        centro.add(menu, BorderLayout.CENTER);

        // --- Rodapé: usuário logado, tema e sair ---
        JPanel rodape = new JPanel();
        rodape.setOpaque(false);
        rodape.setLayout(new BoxLayout(rodape, BoxLayout.Y_AXIS));

        JLabel usuario = new JLabel(funcionarioLogado.getNome());
        usuario.putClientProperty(FlatClientProperties.STYLE_CLASS, "h4");
        usuario.setForeground(Color.WHITE);
        JLabel cargo = new JLabel(funcionarioLogado.getCargo());
        cargo.setForeground(new Color(0xC8D0B4));

        botaoTema = criarBotaoRodape(textoBotaoTema());
        botaoTema.addActionListener(e -> {
            Tema.alternar();
            botaoTema.setText(textoBotaoTema());
        });

        JButton sair = criarBotaoRodape("Sair");
        sair.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });

        rodape.add(usuario);
        rodape.add(cargo);
        rodape.add(Box.createVerticalStrut(14));
        rodape.add(botaoTema);
        rodape.add(Box.createVerticalStrut(6));
        rodape.add(sair);

        barra.add(centro, BorderLayout.CENTER);
        barra.add(rodape, BorderLayout.SOUTH);
        return barra;
    }

    private JToggleButton criarItemMenu(String texto, String nomeTela) {
        JToggleButton item = new JToggleButton(texto);
        configurarBotaoBarra(item);
        grupoMenu.add(item);   // ButtonGroup: só um selecionado por vez
        item.addActionListener(e -> mostrar(nomeTela));
        return item;
    }

    private JButton criarBotaoRodape(String texto) {
        JButton botao = new JButton(texto);
        configurarBotaoBarra(botao);
        return botao;
    }

    // AbstractButton é a mãe de JButton e JToggleButton: um método serve aos dois
    private void configurarBotaoBarra(AbstractButton botao) {
        botao.putClientProperty(FlatClientProperties.STYLE, ESTILO_ITEM_MENU);
        botao.setHorizontalAlignment(SwingConstants.LEFT);
        botao.setMargin(new Insets(9, 14, 9, 14));
        botao.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        botao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        botao.setFocusable(false);
    }

    private String textoBotaoTema() {
        return Tema.isEscuro() ? "Tema claro" : "Tema escuro";
    }
}