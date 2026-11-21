package br.com.biblioteca.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

import com.formdev.flatlaf.FlatClientProperties;

import br.com.biblioteca.exception.NegocioException;
import br.com.biblioteca.model.Copia;
import br.com.biblioteca.model.Emprestimo;
import br.com.biblioteca.model.Funcionario;
import br.com.biblioteca.model.Leitor;
import br.com.biblioteca.model.SituacaoCopia;
import br.com.biblioteca.service.EmprestimoService;
import br.com.biblioteca.service.LeitorService;
import br.com.biblioteca.service.ObraService;

/** Tela de empréstimos e devoluções (RN01 a RN07 e RN09). */
public class EmprestimosPanel extends PainelTela {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private final Funcionario funcionarioLogado;   // RN09
    private final EmprestimoService emprestimoService = new EmprestimoService();
    private final LeitorService leitorService = new LeitorService();
    private final ObraService obraService = new ObraService();

    // ---- Tabela ----
    private final ModeloTabela<Emprestimo> modelo = new ModeloTabela<>(
            "Cópia", "Obra", "Leitor", "Prevista", "Devolução", "Situação", "Multa") {
        private static final long serialVersionUID = 1L;

        @Override
        protected Object valorDaColuna(Emprestimo e, int coluna) {
            return switch (coluna) {
                case 0 -> e.getCopia().getCodigo();
                case 1 -> e.getCopia().getObra().getTitulo();
                case 2 -> e.getLeitor().getNome();
                case 3 -> e.getDataPrevista().format(DATA);
                case 4 -> e.getDataDevolucao() == null ? "—" : e.getDataDevolucao().format(DATA);
                case 5 -> situacao(e);
                case 6 -> textoMulta(e);
                default -> "";
            };
        }
    };
    private final JTable tabela = new JTable(modelo);
    private final TableRowSorter<ModeloTabela<Emprestimo>> ordenador = new TableRowSorter<>(modelo);
    private final JTextField campoBusca = new JTextField(20);
    private final JComboBox<String> comboFiltro =
        new JComboBox<>(new String[] {"Todos", "Ativos", "Em atraso", "Devolvidos"});

    // ---- Formulários ----
    private final JComboBox<Leitor> comboLeitor = new JComboBox<>();
    private final JLabel infoLeitor = new JLabel(" ");
    private final JTextField campoCopiaEmprestimo = new JTextField();
    private final JTextField campoCopiaDevolucao = new JTextField();

    public EmprestimosPanel(Funcionario funcionarioLogado) {
        this.funcionarioLogado = funcionarioLogado;
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));
        add(montarTopo(), BorderLayout.NORTH);
        add(montarTabela(), BorderLayout.CENTER);
        add(montarFormularios(), BorderLayout.EAST);
    }

    @Override
    public void atualizar() {
        try {
            carregarLeitores();
            modelo.setItens(emprestimoService.listarTodos());
            filtrar();
        // >>> TRATAMENTO DE EXCEÇÕES NA UI
        } catch (SQLException e) {
            mostrarErroTecnico(e);
        }
    }

    // ===================== Montagem da tela =====================

    private JPanel montarTopo() {
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.add(criarCabecalho("Empréstimos", "Registre saídas e devoluções de cópias"), BorderLayout.WEST);

        campoBusca.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por cópia, obra ou leitor");
        campoBusca.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        campoBusca.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
        comboFiltro.addActionListener(e -> filtrar());

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);
        acoes.add(campoBusca);
        acoes.add(comboFiltro);
        topo.add(acoes, BorderLayout.SOUTH);
        return topo;
    }

    private JPanel montarTabela() {
        tabela.setRowSorter(ordenador);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setShowVerticalLines(false);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.getColumnModel().getColumn(5).setCellRenderer(new SeloRenderer(Map.of(
            "ATIVO", Tema.AZUL,
            "ATRASADO", Tema.VERMELHO,
            "DEVOLVIDO", new Color(0x757575))));

        int[] larguras = {60, 160, 110, 75, 80, 110, 95};
        for (int i = 0; i < larguras.length; i++) {
            tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        }

        // Ao selecionar um empréstimo ativo, o código já vai para o campo de devolução
        tabela.getSelectionModel().addListSelectionListener(e -> {
            int linhaVisual = tabela.getSelectedRow();
            if (!e.getValueIsAdjusting() && linhaVisual >= 0) {
                Emprestimo emp = modelo.getItem(tabela.convertRowIndexToModel(linhaVisual));
                if (!emp.isDevolvido()) {
                    campoCopiaDevolucao.setText(emp.getCopia().getCodigo());
                }
            }
        });

        JLabel legenda = new JLabel("* multa acumulada até hoje (empréstimo ainda não devolvido)");
        legenda.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
        legenda.setEnabled(false);

        JPanel painel = new JPanel(new BorderLayout(0, 8));
        painel.setOpaque(false);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);
        painel.add(legenda, BorderLayout.SOUTH);
        return painel;
    }

    private JPanel montarFormularios() {
        PainelCartao cartao = new PainelCartao();
        cartao.setLayout(new GridBagLayout());
        cartao.setPreferredSize(new Dimension(310, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        // --- Novo empréstimo ---
        JLabel tituloEmprestimo = new JLabel("Novo empréstimo");
        tituloEmprestimo.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3");
        c.insets = new Insets(0, 0, 16, 0);
        cartao.add(tituloEmprestimo, c);

        comboLeitor.addActionListener(e -> atualizarInfoLeitor());
        adicionarCampo(cartao, c, "Leitor", comboLeitor);

        infoLeitor.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
        c.gridy++;
        c.insets = new Insets(-6, 0, 12, 0);
        cartao.add(infoLeitor, c);

        campoCopiaEmprestimo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "ex.: C003");
        adicionarCampo(cartao, c, "Código da cópia", campoCopiaEmprestimo);

        JButton botaoEmprestar = new JButton("Emprestar");
        botaoEmprestar.putClientProperty(FlatClientProperties.STYLE, "font: bold");
        botaoEmprestar.addActionListener(e -> emprestar());
        c.gridy++;
        c.insets = new Insets(4, 0, 20, 0);
        cartao.add(botaoEmprestar, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 16, 0);
        cartao.add(new JSeparator(), c);

        // --- Devolução ---
        JLabel tituloDevolucao = new JLabel("Devolução");
        tituloDevolucao.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3");
        c.gridy++;
        c.insets = new Insets(0, 0, 16, 0);
        cartao.add(tituloDevolucao, c);

        campoCopiaDevolucao.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "ex.: C001");
        adicionarCampo(cartao, c, "Código da cópia", campoCopiaDevolucao);

        JButton botaoDevolver = new JButton("Registrar devolução");
        botaoDevolver.addActionListener(e -> devolver());
        c.gridy++;
        c.insets = new Insets(4, 0, 0, 0);
        cartao.add(botaoDevolver, c);

        c.gridy++;
        c.weighty = 1;
        cartao.add(new JLabel(), c);   // espaço elástico
        return cartao;
    }

    private void adicionarCampo(JPanel painel, GridBagConstraints c, String rotulo, JComponent campo) {
        c.gridy++;
        c.insets = new Insets(0, 0, 4, 0);
        painel.add(new JLabel(rotulo), c);
        c.gridy++;
        c.insets = new Insets(0, 0, 12, 0);
        painel.add(campo, c);
    }

    // ===================== Ações =====================

    private void emprestar() {
        Leitor leitor = (Leitor) comboLeitor.getSelectedItem();
        if (leitor == null) {
            aviso("Cadastre um leitor antes de registrar empréstimos.");
            return;
        }
        try {
            Copia copia = buscarCopia(campoCopiaEmprestimo);
            if (copia == null) {
                return;
            }
            // O service confere todas as regras e lança NegocioException se alguma falhar
            Emprestimo emp = emprestimoService.realizarEmprestimo(
                leitor, copia, funcionarioLogado, LocalDate.now());

            JOptionPane.showMessageDialog(this,
                "Empréstimo registrado!\n\n"
                + copia + "\npara " + leitor.getNome()
                + "\n\nDevolver até " + emp.getDataPrevista().format(DATA),
                "Empréstimos", JOptionPane.INFORMATION_MESSAGE);
            campoCopiaEmprestimo.setText("");
            atualizar();

        // >>> TRATAMENTO DE EXCEÇÕES NA UI
        } catch (NegocioException e) {
            // Atraso, limite atingido, cópia indisponível ou reservada para outro leitor
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Empréstimo não permitido", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            mostrarErroTecnico(e);
        }
    }

    private void devolver() {
        try {
            Copia copia = buscarCopia(campoCopiaDevolucao);
            if (copia == null) {
                return;
            }
            Emprestimo emp = emprestimoService.registrarDevolucao(copia, LocalDate.now());

            StringBuilder mensagem = new StringBuilder("Devolução registrada: ")
                .append(emp.getCopia()).append("\n\n");
            // BigDecimal: signum() devolve -1, 0 ou 1 (negativo, zero ou positivo)
            if (emp.getMulta().signum() > 0) {
                mensagem.append("Multa por atraso: ").append(MOEDA.format(emp.getMulta()));
            } else {
                mensagem.append("Devolvido no prazo, sem multa.");
            }
            if (emp.getCopia().getSituacao() == SituacaoCopia.RESERVADA) {
                mensagem.append("\n\nHá reserva para esta obra: a cópia ficou RESERVADA\n"
                    + "para o primeiro leitor da fila.");
            }
            JOptionPane.showMessageDialog(this, mensagem.toString(),
                "Devoluções", JOptionPane.INFORMATION_MESSAGE);
            campoCopiaDevolucao.setText("");
            atualizar();

        // >>> TRATAMENTO DE EXCEÇÕES NA UI
        } catch (NegocioException e) {
            // Ex.: cópia sem empréstimo ativo
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Devolução não permitida", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            mostrarErroTecnico(e);
        }
    }

    // ===================== Auxiliares =====================

    /** Procura a cópia pelo código digitado; avisa e devolve null se não achar. */
    private Copia buscarCopia(JTextField campo) throws SQLException {
        String codigo = campo.getText().trim();
        if (codigo.isEmpty()) {
            aviso("Informe o código da cópia.");
            return null;
        }
        Copia copia = obraService.buscarCopiaPorCodigo(codigo);
        if (copia == null) {
            aviso("Nenhuma cópia com o código " + codigo + ".");
        }
        return copia;
    }

    private void carregarLeitores() throws SQLException {
        Leitor anterior = (Leitor) comboLeitor.getSelectedItem();
        DefaultComboBoxModel<Leitor> modeloCombo = new DefaultComboBoxModel<>();
        for (Leitor l : leitorService.listarTodos()) {
            modeloCombo.addElement(l);   // o combo exibe o toString(): o nome
            if (anterior != null && anterior.getId().equals(l.getId())) {
                modeloCombo.setSelectedItem(l);   // mantém o leitor escolhido
            }
        }
        comboLeitor.setModel(modeloCombo);
        atualizarInfoLeitor();
    }

    /** Mostra prazo e limite do leitor escolhido: o POLIMORFISMO visível. */
    private void atualizarInfoLeitor() {
        Leitor l = (Leitor) comboLeitor.getSelectedItem();
        if (l == null) {
            infoLeitor.setText(" ");
        } else {
            infoLeitor.setText(l.getTipo() + " · prazo de " + l.getPrazoDias()
                + " dias · até " + l.getLimiteEmprestimos() + " livros");
        }
    }

    private void filtrar() {
        List<RowFilter<ModeloTabela<Emprestimo>, Integer>> filtros = new ArrayList<>();

        String texto = campoBusca.getText().trim();
        if (!texto.isEmpty()) {
            filtros.add(RowFilter.regexFilter("(?i)" + Pattern.quote(texto), 0, 1, 2));
        }
        switch ((String) comboFiltro.getSelectedItem()) {
            case "Ativos" -> filtros.add(RowFilter.regexFilter("^(ATIVO|ATRASADO)$", 5));
            case "Em atraso" -> filtros.add(RowFilter.regexFilter("^ATRASADO$", 5));
            case "Devolvidos" -> filtros.add(RowFilter.regexFilter("^DEVOLVIDO$", 5));
            default -> { }   // "Todos": sem filtro de situação
        }
        // andFilter: a linha aparece só se passar em TODOS os filtros
        ordenador.setRowFilter(filtros.isEmpty() ? null : RowFilter.andFilter(filtros));
    }

    private static String situacao(Emprestimo e) {
        if (e.isDevolvido()) {
            return "DEVOLVIDO";
        }
        return e.estaAtrasado(LocalDate.now()) ? "ATRASADO" : "ATIVO";
    }

    private static String textoMulta(Emprestimo e) {
        if (e.isDevolvido()) {
            return e.getMulta().signum() == 0 ? "—" : MOEDA.format(e.getMulta());
        }
        BigDecimal ateHoje = e.calcularMulta(LocalDate.now());
        return ateHoje.signum() == 0 ? "—" : MOEDA.format(ateHoje) + " *";
    }

    private void aviso(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarErroTecnico(SQLException e) {
        JOptionPane.showMessageDialog(this, "Erro ao acessar o banco de dados:\n" + e.getMessage(),
            "Erro", JOptionPane.ERROR_MESSAGE);
    }
}