package br.com.biblioteca.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

import com.formdev.flatlaf.FlatClientProperties;

import br.com.biblioteca.exception.NegocioException;
import br.com.biblioteca.model.Aluno;
import br.com.biblioteca.model.Leitor;
import br.com.biblioteca.model.Professor;
import br.com.biblioteca.service.LeitorService;

/** Tela de cadastro de leitores: tabela com busca + formulário lateral. */
public class LeitoresPanel extends PainelTela {

    private static final long serialVersionUID = 1L;

    private final LeitorService leitorService = new LeitorService();

    // Tabela
    private final LeitorTableModel modelo = new LeitorTableModel();
    private final JTable tabela = new JTable(modelo);
    private final TableRowSorter<LeitorTableModel> ordenador = new TableRowSorter<>(modelo);
    private final JTextField campoBusca = new JTextField(24);

    // Formulário
    private final JLabel tituloFormulario = new JLabel("Novo leitor");
    private final JTextField campoNome = new JTextField();
    private final JTextField campoCpf = new JTextField();
    private final JTextField campoEmail = new JTextField();
    private final JTextField campoTelefone = new JTextField();
    private final JTextField campoMatricula = new JTextField();
    private final JComboBox<String> campoTipo = new JComboBox<>(new String[] {"Aluno", "Professor"});

    /** Leitor selecionado na tabela; null significa "cadastrando um novo". */
    private Leitor leitorEmEdicao;

    public LeitoresPanel() {
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));
        add(montarTopo(), BorderLayout.NORTH);
        add(montarTabela(), BorderLayout.CENTER);
        add(montarFormulario(), BorderLayout.EAST);
    }

    @Override
    public void atualizar() {
        carregarLeitores();
    }

    // ===================== Montagem da tela =====================

    private JPanel montarTopo() {
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.add(criarCabecalho("Leitores", "Alunos e professores cadastrados na biblioteca"), BorderLayout.WEST);

        campoBusca.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por nome, CPF ou matrícula");
        campoBusca.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);

        // CLASSE ANÔNIMA: implementa a interface DocumentListener ali mesmo,
        // sem criar um arquivo. Cada método é chamado quando o texto muda.
        campoBusca.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });

        JButton botaoNovo = new JButton("Novo leitor");
        botaoNovo.addActionListener(e -> limparFormulario());

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);
        acoes.add(campoBusca);
        acoes.add(botaoNovo);
        topo.add(acoes, BorderLayout.SOUTH);
        return topo;
    }

    private JScrollPane montarTabela() {
        tabela.setRowSorter(ordenador);   // permite ordenar (clique no cabeçalho) e filtrar
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setShowVerticalLines(false);
        tabela.getTableHeader().setReorderingAllowed(false);

        // Selo colorido na coluna "Tipo"
        tabela.getColumnModel().getColumn(1).setCellRenderer(
            new SeloRenderer(Map.of("ALUNO", Tema.AZUL, "PROFESSOR", Tema.OLIVA_MEDIO)));

        int[] larguras = {170, 130, 90, 130, 80, 80};
        for (int i = 0; i < larguras.length; i++) {
            tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        }

        // Ao selecionar uma linha, carrega o leitor no formulário
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {   // ignora os eventos "intermediários" da seleção
                preencherFormulario();
            }
        });

        return new JScrollPane(tabela);
    }

    private JPanel montarFormulario() {
        PainelCartao cartao = new PainelCartao();
        cartao.setLayout(new GridBagLayout());
        cartao.setPreferredSize(new Dimension(320, 0));

        tituloFormulario.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3");

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 18, 0);
        cartao.add(tituloFormulario, c);

        adicionarCampo(cartao, c, "Nome", campoNome);
        adicionarCampo(cartao, c, "Tipo", campoTipo);
        adicionarCampo(cartao, c, "Matrícula", campoMatricula);
        adicionarCampo(cartao, c, "CPF", campoCpf);
        adicionarCampo(cartao, c, "E-mail", campoEmail);
        adicionarCampo(cartao, c, "Telefone", campoTelefone);

        JButton botaoSalvar = new JButton("Salvar");
        botaoSalvar.putClientProperty(FlatClientProperties.STYLE, "font: bold");
        botaoSalvar.addActionListener(e -> salvar());
        JButton botaoLimpar = new JButton("Limpar");
        botaoLimpar.addActionListener(e -> limparFormulario());

        JPanel linhaBotoes = new JPanel(new GridLayout(1, 2, 8, 0));
        linhaBotoes.setOpaque(false);
        linhaBotoes.add(botaoSalvar);
        linhaBotoes.add(botaoLimpar);

        JButton botaoExcluir = new JButton("Excluir leitor");
        botaoExcluir.putClientProperty(FlatClientProperties.STYLE, "foreground: #E05252");        
        botaoExcluir.addActionListener(e -> excluir());

        c.gridy++;
        c.insets = new Insets(10, 0, 8, 0);
        cartao.add(linhaBotoes, c);
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        cartao.add(botaoExcluir, c);

        // Espaço elástico no fim: empurra tudo para cima
        c.gridy++;
        c.weighty = 1;
        cartao.add(new JLabel(), c);

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

    private void carregarLeitores() {
        try {
            modelo.setLeitores(leitorService.listarTodos());
        // >>> TRATAMENTO DE EXCEÇÕES NA UI
        } catch (SQLException e) {
            mostrarErroTecnico(e);
        }
    }

    private void salvar() {
        try {
            Leitor leitor = montarLeitorDoFormulario();
            leitorService.salvar(leitor);   // o service decide entre INSERT e UPDATE
            JOptionPane.showMessageDialog(this, leitor.getNome() + " foi salvo com sucesso.",
                "Leitores", JOptionPane.INFORMATION_MESSAGE);
            limparFormulario();
            carregarLeitores();

        // >>> TRATAMENTO DE EXCEÇÕES NA UI
        } catch (NegocioException e) {
            // Regra violada (campo vazio, CPF ou matrícula repetidos): aviso
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Verifique os dados", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            mostrarErroTecnico(e);
        }
    }

    private void excluir() {
        if (leitorEmEdicao == null) {
            JOptionPane.showMessageDialog(this, "Selecione um leitor na tabela.",
                "Leitores", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int resposta = JOptionPane.showConfirmDialog(this,
            "Excluir " + leitorEmEdicao.getNome() + "? Esta ação não pode ser desfeita.",
            "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (resposta != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            leitorService.excluir(leitorEmEdicao);
            limparFormulario();
            carregarLeitores();

        // >>> TRATAMENTO DE EXCEÇÕES NA UI
        } catch (NegocioException e) {
            // Ex.: leitor com empréstimos ativos ou histórico
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Não foi possível excluir", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            mostrarErroTecnico(e);
        }
    }

    // ===================== Auxiliares =====================

    /** Cria Aluno ou Professor com os dados digitados (id null = leitor novo). */
    private Leitor montarLeitorDoFormulario() {
        Integer id = (leitorEmEdicao == null) ? null : leitorEmEdicao.getId();
        String nome = campoNome.getText().trim();
        String cpf = campoCpf.getText().trim();
        String email = campoEmail.getText().trim();
        String telefone = campoTelefone.getText().trim();
        String matricula = campoMatricula.getText().trim();

        if ("Aluno".equals(campoTipo.getSelectedItem())) {
            return new Aluno(id, nome, cpf, email, telefone, matricula);
        }
        return new Professor(id, nome, cpf, email, telefone, matricula);
    }

    private void preencherFormulario() {
        int linhaVisual = tabela.getSelectedRow();
        if (linhaVisual < 0) {
            return;   // nenhuma linha selecionada
        }
        // Com ordenação/filtro, a linha VISUAL pode ser diferente da linha do MODELO
        int linhaModelo = tabela.convertRowIndexToModel(linhaVisual);
        leitorEmEdicao = modelo.getLeitor(linhaModelo);

        tituloFormulario.setText("Editando: " + leitorEmEdicao.getNome());
        campoNome.setText(leitorEmEdicao.getNome());
        campoCpf.setText(leitorEmEdicao.getCpf());
        campoEmail.setText(leitorEmEdicao.getEmail());
        campoTelefone.setText(leitorEmEdicao.getTelefone());
        campoMatricula.setText(leitorEmEdicao.getMatricula());
        campoTipo.setSelectedItem("ALUNO".equals(leitorEmEdicao.getTipo()) ? "Aluno" : "Professor");
    }

    private void limparFormulario() {
        leitorEmEdicao = null;
        tituloFormulario.setText("Novo leitor");
        campoNome.setText("");
        campoCpf.setText("");
        campoEmail.setText("");
        campoTelefone.setText("");
        campoMatricula.setText("");
        campoTipo.setSelectedIndex(0);
        tabela.clearSelection();
        campoNome.requestFocusInWindow();
    }

    private void filtrar() {
        String texto = campoBusca.getText().trim();
        if (texto.isEmpty()) {
            ordenador.setRowFilter(null);   // sem filtro
        } else {
            // (?i) = ignora maiúsculas/minúsculas; Pattern.quote trata o texto literalmente
            ordenador.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(texto)));
        }
    }

    private void mostrarErroTecnico(SQLException e) {
        JOptionPane.showMessageDialog(this, "Erro ao acessar o banco de dados:\n" + e.getMessage(),
            "Erro", JOptionPane.ERROR_MESSAGE);
    }
}