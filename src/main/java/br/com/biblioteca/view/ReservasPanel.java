package br.com.biblioteca.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
import br.com.biblioteca.model.Leitor;
import br.com.biblioteca.model.Obra;
import br.com.biblioteca.model.Reserva;
import br.com.biblioteca.service.LeitorService;
import br.com.biblioteca.service.ObraService;
import br.com.biblioteca.service.ReservaService;

/** Tela de reservas: fila por obra (RN05 e RN06). */
public class ReservasPanel extends PainelTela {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yy");

    private final ReservaService reservaService = new ReservaService();
    private final LeitorService leitorService = new LeitorService();
    private final ObraService obraService = new ObraService();

    /** Posição de cada reserva ATIVA na fila da sua obra (id da reserva -> posição). */
    private final Map<Integer, Integer> posicoesNaFila = new HashMap<>();

    // ---- Tabela ----
    private final ModeloTabela<Reserva> modelo = new ModeloTabela<>(
            "Obra", "Leitor", "Data", "Situação", "Fila") {
        private static final long serialVersionUID = 1L;

        @Override
        protected Object valorDaColuna(Reserva r, int coluna) {
            return switch (coluna) {
                case 0 -> r.getObra().getTitulo();
                case 1 -> r.getLeitor().getNome();
                case 2 -> r.getDataReserva().format(DATA);
                case 3 -> r.getSituacao().name();
                case 4 -> r.isAtiva() ? posicoesNaFila.get(r.getId()) + "º" : "—";
                default -> "";
            };
        }
    };
    private final JTable tabela = new JTable(modelo);
    private final TableRowSorter<ModeloTabela<Reserva>> ordenador = new TableRowSorter<>(modelo);
    private final JTextField campoBusca = new JTextField(20);
    private final JComboBox<String> comboFiltro =
        new JComboBox<>(new String[] {"Todas", "Ativas", "Atendidas", "Canceladas"});

    // ---- Formulário ----
    private final JComboBox<Leitor> comboLeitor = new JComboBox<>();
    private final JComboBox<Obra> comboObra = new JComboBox<>();
    private final JLabel infoObra = new JLabel(" ");

    public ReservasPanel() {
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));
        add(montarTopo(), BorderLayout.NORTH);
        add(montarTabela(), BorderLayout.CENTER);
        add(montarFormulario(), BorderLayout.EAST);
    }

    @Override
    public void atualizar() {
        try {
            carregarCombos();
            List<Reserva> reservas = reservaService.listarTodas();
            calcularFilas(reservas);
            modelo.setItens(reservas);
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
        topo.add(criarCabecalho("Reservas", "Fila de espera por obras sem cópia disponível"), BorderLayout.WEST);

        campoBusca.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por obra ou leitor");
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

    private JScrollPane montarTabela() {
        tabela.setRowSorter(ordenador);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setShowVerticalLines(false);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.getColumnModel().getColumn(3).setCellRenderer(new SeloRenderer(Map.of(
            "ATIVA", Tema.AZUL,
            "ATENDIDA", Tema.VERDE,
            "CANCELADA", new Color(0x757575))));

        int[] larguras = {200, 140, 80, 110, 60};
        for (int i = 0; i < larguras.length; i++) {
            tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        }
        return new JScrollPane(tabela);
    }

    private JPanel montarFormulario() {
        PainelCartao cartao = new PainelCartao();
        cartao.setLayout(new GridBagLayout());
        cartao.setPreferredSize(new Dimension(310, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        JLabel titulo = new JLabel("Nova reserva");
        titulo.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3");
        c.insets = new Insets(0, 0, 16, 0);
        cartao.add(titulo, c);

        adicionarCampo(cartao, c, "Leitor", comboLeitor);

        comboObra.addActionListener(e -> atualizarInfoObra());
        adicionarCampo(cartao, c, "Obra", comboObra);

        infoObra.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
        c.gridy++;
        c.insets = new Insets(-6, 0, 12, 0);
        cartao.add(infoObra, c);

        JButton botaoReservar = new JButton("Reservar");
        botaoReservar.putClientProperty(FlatClientProperties.STYLE, "font: bold");
        botaoReservar.addActionListener(e -> reservar());
        c.gridy++;
        c.insets = new Insets(4, 0, 20, 0);
        cartao.add(botaoReservar, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 16, 0);
        cartao.add(new JSeparator(), c);

        JLabel tituloCancelar = new JLabel("Cancelar reserva");
        tituloCancelar.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3");
        c.gridy++;
        c.insets = new Insets(0, 0, 8, 0);
        cartao.add(tituloCancelar, c);

        JLabel dica = new JLabel("<html>Selecione uma reserva ATIVA na tabela.</html>");
        dica.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
        c.gridy++;
        c.insets = new Insets(0, 0, 12, 0);
        cartao.add(dica, c);

        JButton botaoCancelar = new JButton("Cancelar reserva selecionada");
        botaoCancelar.putClientProperty(FlatClientProperties.STYLE, "foreground: #E05252");
        botaoCancelar.addActionListener(e -> cancelar());
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        cartao.add(botaoCancelar, c);

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

    private void reservar() {
        Leitor leitor = (Leitor) comboLeitor.getSelectedItem();
        Obra obra = (Obra) comboObra.getSelectedItem();
        if (leitor == null || obra == null) {
            aviso("Escolha o leitor e a obra.");
            return;
        }
        try {
            reservaService.reservar(leitor, obra, LocalDate.now());
            JOptionPane.showMessageDialog(this,
                leitor.getNome() + " entrou na fila de \"" + obra.getTitulo() + "\".",
                "Reservas", JOptionPane.INFORMATION_MESSAGE);
            atualizar();
        // >>> TRATAMENTO DE EXCEÇÕES NA UI
        } catch (NegocioException e) {
            // RN05 (há cópia disponível) ou leitor já na fila
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Reserva não permitida", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            mostrarErroTecnico(e);
        }
    }

    private void cancelar() {
        int linhaVisual = tabela.getSelectedRow();
        if (linhaVisual < 0) {
            aviso("Selecione uma reserva na tabela.");
            return;
        }
        Reserva reserva = modelo.getItem(tabela.convertRowIndexToModel(linhaVisual));

        int resposta = JOptionPane.showConfirmDialog(this,
            "Cancelar a reserva de " + reserva.getLeitor().getNome()
                + " para \"" + reserva.getObra().getTitulo() + "\"?",
            "Confirmar cancelamento", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (resposta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            reservaService.cancelar(reserva);
            atualizar();
        // >>> TRATAMENTO DE EXCEÇÕES NA UI
        } catch (NegocioException e) {
            // Ex.: reserva já atendida ou já cancelada
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Não foi possível cancelar", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            mostrarErroTecnico(e);
        }
    }

    // ===================== Auxiliares =====================

    /**
     * Calcula a posição de cada reserva ativa na fila da sua obra (RN06):
     * ordena por data (e id, para desempatar) e numera 1, 2, 3... por obra.
     */
    private void calcularFilas(List<Reserva> reservas) {
        posicoesNaFila.clear();

        List<Reserva> ativas = new ArrayList<>();
        for (Reserva r : reservas) {
            if (r.isAtiva()) {
                ativas.add(r);
            }
        }
        // Comparator com referências a métodos: "por data; empate -> por id"
        ativas.sort(Comparator.comparing(Reserva::getDataReserva).thenComparing(Reserva::getId));

        Map<Integer, Integer> contadorPorObra = new HashMap<>();
        for (Reserva r : ativas) {
            // merge: se a obra ainda não tem contador, começa em 1; senão, soma 1
            int posicao = contadorPorObra.merge(r.getObra().getId(), 1, Integer::sum);
            posicoesNaFila.put(r.getId(), posicao);
        }
    }

    private void carregarCombos() throws SQLException {
        Leitor leitorAnterior = (Leitor) comboLeitor.getSelectedItem();
        DefaultComboBoxModel<Leitor> modeloLeitores = new DefaultComboBoxModel<>();
        for (Leitor l : leitorService.listarTodos()) {
            modeloLeitores.addElement(l);
            if (leitorAnterior != null && leitorAnterior.getId().equals(l.getId())) {
                modeloLeitores.setSelectedItem(l);
            }
        }
        comboLeitor.setModel(modeloLeitores);

        Obra obraAnterior = (Obra) comboObra.getSelectedItem();
        DefaultComboBoxModel<Obra> modeloObras = new DefaultComboBoxModel<>();
        for (Obra o : obraService.listarTodas()) {
            modeloObras.addElement(o);   // exibe o toString(): o título
            if (obraAnterior != null && obraAnterior.getId().equals(o.getId())) {
                modeloObras.setSelectedItem(o);
            }
        }
        comboObra.setModel(modeloObras);
        atualizarInfoObra();
    }

    /** Mostra quantas cópias da obra estão livres: explica a RN05 antes do clique. */
    private void atualizarInfoObra() {
        Obra obra = (Obra) comboObra.getSelectedItem();
        if (obra == null) {
            infoObra.setText(" ");
            return;
        }
        int disponiveis = 0;
        for (Copia copia : obra.getCopias()) {
            if (copia.isDisponivel()) {
                disponiveis++;
            }
        }
        infoObra.setText(obra.getCopias().size() + " cópias · " + disponiveis + " disponíveis"
            + (disponiveis > 0 ? " (empreste em vez de reservar)" : ""));
    }

    private void filtrar() {
        List<RowFilter<ModeloTabela<Reserva>, Integer>> filtros = new ArrayList<>();

        String texto = campoBusca.getText().trim();
        if (!texto.isEmpty()) {
            filtros.add(RowFilter.regexFilter("(?i)" + Pattern.quote(texto), 0, 1));
        }
        switch ((String) comboFiltro.getSelectedItem()) {
            case "Ativas" -> filtros.add(RowFilter.regexFilter("^ATIVA$", 3));
            case "Atendidas" -> filtros.add(RowFilter.regexFilter("^ATENDIDA$", 3));
            case "Canceladas" -> filtros.add(RowFilter.regexFilter("^CANCELADA$", 3));
            default -> { }
        }
        ordenador.setRowFilter(filtros.isEmpty() ? null : RowFilter.andFilter(filtros));
    }

    private void aviso(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarErroTecnico(SQLException e) {
        JOptionPane.showMessageDialog(this, "Erro ao acessar o banco de dados:\n" + e.getMessage(),
            "Erro", JOptionPane.ERROR_MESSAGE);
    }
}