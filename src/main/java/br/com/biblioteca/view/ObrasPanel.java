package br.com.biblioteca.view;

import java.awt.BorderLayout;
import java.awt.Color;
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
import br.com.biblioteca.model.Obra;
import br.com.biblioteca.service.ObraService;

/**
 * Tela de obras e cópias (mestre-detalhe).
 * Mostra a AGREGAÇÃO: ao selecionar uma Obra, aparecem as Cópias que ela agrega.
 */
public class ObrasPanel extends PainelTela {

    private static final long serialVersionUID = 1L;

    private final ObraService obraService = new ObraService();

    // ---- Tabela de obras (CLASSE ANÔNIMA que estende ModeloTabela<Obra>) ----
    private final ModeloTabela<Obra> modeloObras =
        new ModeloTabela<>("Título", "Autor", "Ano", "Cópias", "Disponíveis") {
            private static final long serialVersionUID = 1L;

            @Override
            protected Object valorDaColuna(Obra obra, int coluna) {
                return switch (coluna) {
                    case 0 -> obra.getTitulo();
                    case 1 -> obra.getAutor();
                    case 2 -> obra.getAno();
                    case 3 -> obra.getCopias().size();
                    case 4 -> contarDisponiveis(obra);
                    default -> "";
                };
            }
        };
    private final JTable tabelaObras = new JTable(modeloObras);
    private final TableRowSorter<ModeloTabela<Obra>> ordenador = new TableRowSorter<>(modeloObras);
    private final JTextField campoBusca = new JTextField(24);

    // ---- Tabela de cópias da obra selecionada ----
    private final ModeloTabela<Copia> modeloCopias =
        new ModeloTabela<>("Código", "Situação") {
            private static final long serialVersionUID = 1L;

            @Override
            protected Object valorDaColuna(Copia copia, int coluna) {
                return coluna == 0 ? copia.getCodigo() : copia.getSituacao().name();
            }
        };
    private final JTable tabelaCopias = new JTable(modeloCopias);
    private final JLabel tituloCopias = new JLabel("Cópias");

    // ---- Formulário ----
    private final JLabel tituloFormulario = new JLabel("Nova obra");
    private final JTextField campoTitulo = new JTextField();
    private final JTextField campoAutor = new JTextField();
    private final JTextField campoEditora = new JTextField();
    private final JTextField campoAno = new JTextField();
    private final JTextField campoIsbn = new JTextField();
    private final JTextField campoCodigoCopia = new JTextField();
    private final JButton botaoAdicionarCopia = new JButton("Adicionar cópia");

    /** Obra selecionada; null = cadastrando uma nova. */
    private Obra obraEmEdicao;

    public ObrasPanel() {
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));
        add(montarTopo(), BorderLayout.NORTH);
        add(montarTabelas(), BorderLayout.CENTER);
        add(montarFormulario(), BorderLayout.EAST);
        atualizarAreaCopias();
    }

    @Override
    public void atualizar() {
        carregarObras();
    }

    // ===================== Montagem da tela =====================

    private JPanel montarTopo() {
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.add(criarCabecalho("Obras e cópias", "O acervo: cada obra e seus exemplares físicos"),
            BorderLayout.WEST);

        campoBusca.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por título ou autor");
        campoBusca.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        campoBusca.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });

        JButton botaoNova = new JButton("Nova obra");
        botaoNova.addActionListener(e -> limparFormulario());

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);
        acoes.add(campoBusca);
        acoes.add(botaoNova);
        topo.add(acoes, BorderLayout.SOUTH);
        return topo;
    }

    private JPanel montarTabelas() {
        // Tabela de obras
        tabelaObras.setRowSorter(ordenador);
        tabelaObras.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaObras.setShowVerticalLines(false);
        tabelaObras.getTableHeader().setReorderingAllowed(false);
        int[] larguras = {220, 170, 60, 70, 90};
        for (int i = 0; i < larguras.length; i++) {
            tabelaObras.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        }
        tabelaObras.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                preencherFormulario();
            }
        });

        // Tabela de cópias, com selos de situação
        tabelaCopias.setShowVerticalLines(false);
        tabelaCopias.setRowSelectionAllowed(false);
        tabelaCopias.getTableHeader().setReorderingAllowed(false);
        tabelaCopias.getColumnModel().getColumn(1).setCellRenderer(new SeloRenderer(Map.of(
            "DISPONIVEL", Tema.VERDE,
            "EMPRESTADA", Tema.LARANJA,
            "RESERVADA", Tema.AZUL,
            "MANUTENCAO", new Color(0x757575))));

        tituloCopias.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3");

        JPanel areaCopias = new JPanel(new BorderLayout(0, 8));
        areaCopias.setOpaque(false);
        areaCopias.setPreferredSize(new Dimension(0, 230));
        areaCopias.add(tituloCopias, BorderLayout.NORTH);
        areaCopias.add(new JScrollPane(tabelaCopias), BorderLayout.CENTER);

        JPanel tabelas = new JPanel(new BorderLayout(0, 18));
        tabelas.setOpaque(false);
        tabelas.add(new JScrollPane(tabelaObras), BorderLayout.CENTER);
        tabelas.add(areaCopias, BorderLayout.SOUTH);
        return tabelas;
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

        adicionarCampo(cartao, c, "Título", campoTitulo);
        adicionarCampo(cartao, c, "Autor", campoAutor);
        adicionarCampo(cartao, c, "Editora", campoEditora);

        // Ano e ISBN lado a lado
        JPanel anoIsbn = new JPanel(new GridLayout(2, 2, 8, 4));
        anoIsbn.setOpaque(false);
        anoIsbn.add(new JLabel("Ano"));
        anoIsbn.add(new JLabel("ISBN"));
        anoIsbn.add(campoAno);
        anoIsbn.add(campoIsbn);
        c.gridy++;
        c.insets = new Insets(0, 0, 12, 0);
        cartao.add(anoIsbn, c);

        JButton botaoSalvar = new JButton("Salvar obra");
        botaoSalvar.putClientProperty(FlatClientProperties.STYLE, "font: bold");
        botaoSalvar.addActionListener(e -> salvarObra());
        JButton botaoExcluir = new JButton("Excluir");
        botaoExcluir.putClientProperty(FlatClientProperties.STYLE, "foreground: #E05252");
        botaoExcluir.addActionListener(e -> excluirObra());

        JPanel linhaBotoes = new JPanel(new GridLayout(1, 2, 8, 0));
        linhaBotoes.setOpaque(false);
        linhaBotoes.add(botaoSalvar);
        linhaBotoes.add(botaoExcluir);
        c.gridy++;
        c.insets = new Insets(6, 0, 18, 0);
        cartao.add(linhaBotoes, c);

        // --- Seção de cópias ---
        c.gridy++;
        c.insets = new Insets(0, 0, 14, 0);
        cartao.add(new JSeparator(), c);

        JLabel tituloNovaCopia = new JLabel("Adicionar cópia");
        tituloNovaCopia.putClientProperty(FlatClientProperties.STYLE_CLASS, "h4");
        c.gridy++;
        c.insets = new Insets(0, 0, 8, 0);
        cartao.add(tituloNovaCopia, c);

        campoCodigoCopia.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Código, ex.: C003");
        botaoAdicionarCopia.addActionListener(e -> adicionarCopia());
        JPanel linhaCopia = new JPanel(new BorderLayout(8, 0));
        linhaCopia.setOpaque(false);
        linhaCopia.add(campoCodigoCopia, BorderLayout.CENTER);
        linhaCopia.add(botaoAdicionarCopia, BorderLayout.EAST);
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        cartao.add(linhaCopia, c);

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

    private void carregarObras() {
        Integer idSelecionado = (obraEmEdicao == null) ? null : obraEmEdicao.getId();
        try {
            modeloObras.setItens(obraService.listarTodas());
            selecionarObraPorId(idSelecionado);   // mantém a mesma obra selecionada
        // >>> TRATAMENTO DE EXCEÇÕES NA UI
        } catch (SQLException e) {
            mostrarErroTecnico(e);
        }
    }

    private void salvarObra() {
        try {
            // Converte o ano ANTES de alterar o objeto: se falhar, nada muda
            int ano = Integer.parseInt(campoAno.getText().trim());

            Obra obra = (obraEmEdicao == null) ? new Obra("", "", "", 0, "") : obraEmEdicao;
            obra.setTitulo(campoTitulo.getText().trim());
            obra.setAutor(campoAutor.getText().trim());
            obra.setEditora(campoEditora.getText().trim());
            obra.setAno(ano);
            obra.setIsbn(campoIsbn.getText().trim());

            obraService.salvar(obra);
            obraEmEdicao = obra;   // a obra salva fica selecionada, pronta para receber cópias
            JOptionPane.showMessageDialog(this, "\"" + obra.getTitulo() + "\" foi salva.",
                "Obras", JOptionPane.INFORMATION_MESSAGE);
            carregarObras();

        // >>> TRATAMENTO DE EXCEÇÕES NA UI
        } catch (NumberFormatException e) {
            // Exceção UNCHECKED do Java: o texto do ano não é um número
            JOptionPane.showMessageDialog(this, "Informe o ano com números, ex.: 1899.",
                "Verifique os dados", JOptionPane.WARNING_MESSAGE);
        } catch (NegocioException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Verifique os dados", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            mostrarErroTecnico(e);
        }
    }

    private void excluirObra() {
        if (obraEmEdicao == null) {
            JOptionPane.showMessageDialog(this, "Selecione uma obra na tabela.",
                "Obras", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int resposta = JOptionPane.showConfirmDialog(this,
            "Excluir \"" + obraEmEdicao.getTitulo() + "\"?",
            "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (resposta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            obraService.excluir(obraEmEdicao);
            limparFormulario();
            carregarObras();
        // >>> TRATAMENTO DE EXCEÇÕES NA UI
        } catch (NegocioException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Não foi possível excluir", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            mostrarErroTecnico(e);
        }
    }

    private void adicionarCopia() {
        if (obraEmEdicao == null || obraEmEdicao.getId() == null) {
            JOptionPane.showMessageDialog(this, "Selecione ou salve uma obra antes de adicionar cópias.",
                "Cópias", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            Copia copia = obraService.adicionarCopia(obraEmEdicao, campoCodigoCopia.getText());
            campoCodigoCopia.setText("");
            JOptionPane.showMessageDialog(this, "Cópia " + copia.getCodigo() + " adicionada.",
                "Cópias", JOptionPane.INFORMATION_MESSAGE);
            carregarObras();   // atualiza contagens e a lista de cópias
        // >>> TRATAMENTO DE EXCEÇÕES NA UI
        } catch (NegocioException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Verifique os dados", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            mostrarErroTecnico(e);
        }
    }

    // ===================== Auxiliares =====================

    private void preencherFormulario() {
        int linhaVisual = tabelaObras.getSelectedRow();
        if (linhaVisual < 0) {
            return;
        }
        obraEmEdicao = modeloObras.getItem(tabelaObras.convertRowIndexToModel(linhaVisual));

        tituloFormulario.setText("Editando obra");
        campoTitulo.setText(obraEmEdicao.getTitulo());
        campoAutor.setText(obraEmEdicao.getAutor());
        campoEditora.setText(obraEmEdicao.getEditora());
        campoAno.setText(String.valueOf(obraEmEdicao.getAno()));
        campoIsbn.setText(obraEmEdicao.getIsbn() == null ? "" : obraEmEdicao.getIsbn());
        atualizarAreaCopias();
    }

    /** Mostra as cópias da obra selecionada (a agregação na tela). */
    private void atualizarAreaCopias() {
        if (obraEmEdicao == null || obraEmEdicao.getId() == null) {
            tituloCopias.setText("Cópias (selecione uma obra)");
            modeloCopias.setItens(java.util.List.of());
            botaoAdicionarCopia.setEnabled(false);
        } else {
            tituloCopias.setText("Cópias de \"" + obraEmEdicao.getTitulo() + "\"");
            modeloCopias.setItens(obraEmEdicao.getCopias());
            botaoAdicionarCopia.setEnabled(true);
        }
    }

    private void selecionarObraPorId(Integer id) {
        if (id == null) {
            atualizarAreaCopias();
            return;
        }
        for (int i = 0; i < modeloObras.getRowCount(); i++) {
            if (id.equals(modeloObras.getItem(i).getId())) {
                int linhaVisual = tabelaObras.convertRowIndexToView(i);
                if (linhaVisual >= 0) {
                    tabelaObras.setRowSelectionInterval(linhaVisual, linhaVisual);  // dispara preencherFormulario
                }
                return;
            }
        }
    }

    private void limparFormulario() {
        obraEmEdicao = null;
        tituloFormulario.setText("Nova obra");
        campoTitulo.setText("");
        campoAutor.setText("");
        campoEditora.setText("");
        campoAno.setText("");
        campoIsbn.setText("");
        campoCodigoCopia.setText("");
        tabelaObras.clearSelection();
        atualizarAreaCopias();
        campoTitulo.requestFocusInWindow();
    }

    private void filtrar() {
        String texto = campoBusca.getText().trim();
        ordenador.setRowFilter(texto.isEmpty() ? null
            : RowFilter.regexFilter("(?i)" + Pattern.quote(texto), 0, 1));   // colunas 0 e 1
    }

    private int contarDisponiveis(Obra obra) {
        int total = 0;
        for (Copia copia : obra.getCopias()) {
            if (copia.isDisponivel()) {
                total++;
            }
        }
        return total;
    }

    private void mostrarErroTecnico(SQLException e) {
        JOptionPane.showMessageDialog(this, "Erro ao acessar o banco de dados:\n" + e.getMessage(),
            "Erro", JOptionPane.ERROR_MESSAGE);
    }
}