package br.com.biblioteca.view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.sql.SQLException;
import java.time.LocalDate;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.formdev.flatlaf.FlatClientProperties;

import br.com.biblioteca.model.Copia;
import br.com.biblioteca.model.Emprestimo;
import br.com.biblioteca.model.Funcionario;
import br.com.biblioteca.model.Obra;
import br.com.biblioteca.service.EmprestimoService;
import br.com.biblioteca.service.ObraService;

/** Painel inicial com os indicadores da biblioteca. */
public class InicioPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ObraService obraService = new ObraService();
    private final EmprestimoService emprestimoService = new EmprestimoService();

    private final CartaoIndicador cartaoObras = new CartaoIndicador("Obras", Tema.OLIVA);
    private final CartaoIndicador cartaoDisponiveis = new CartaoIndicador("Cópias disponíveis", Tema.VERDE);
    private final CartaoIndicador cartaoAtivos = new CartaoIndicador("Empréstimos ativos", Tema.LARANJA);
    private final CartaoIndicador cartaoAtrasados = new CartaoIndicador("Em atraso", Tema.VERMELHO);

    public InicioPanel(Funcionario funcionario) {
        setLayout(new BorderLayout(0, 24));
        setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));

        JPanel cabecalho = new JPanel(new GridLayout(2, 1));
        cabecalho.setOpaque(false);
        JLabel titulo = new JLabel("Início");
        titulo.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");
        JLabel saudacao = new JLabel("Olá, " + funcionario.getNome() + ". Este é o resumo da biblioteca hoje.");
        saudacao.putClientProperty(FlatClientProperties.STYLE_CLASS, "medium");
        cabecalho.add(titulo);
        cabecalho.add(saudacao);

        JPanel grade = new JPanel(new GridLayout(1, 4, 16, 0));
        grade.setOpaque(false);
        grade.add(cartaoObras);
        grade.add(cartaoDisponiveis);
        grade.add(cartaoAtivos);
        grade.add(cartaoAtrasados);

        add(cabecalho, BorderLayout.NORTH);
        grade.setPreferredSize(new Dimension(0, 140));   // altura fixa dos cartões

        JPanel areaCartoes = new JPanel(new BorderLayout());
        areaCartoes.setOpaque(false);
        areaCartoes.add(grade, BorderLayout.NORTH);      // NORTH respeita a altura preferida
        add(areaCartoes, BorderLayout.CENTER);    
    }

    /** Recalcula os números. Chamado sempre que o painel é exibido. */
    public void atualizar() {
        try {
            int obras = 0, copias = 0, disponiveis = 0;
            for (Obra obra : obraService.listarTodas()) {
                obras++;
                for (Copia copia : obra.getCopias()) {
                    copias++;
                    if (copia.isDisponivel()) {
                        disponiveis++;
                    }
                }
            }

            int ativos = 0, atrasados = 0;
            LocalDate hoje = LocalDate.now();
            for (Emprestimo e : emprestimoService.listarTodos()) {
                if (!e.isDevolvido()) {
                    ativos++;
                    if (e.estaAtrasado(hoje)) {
                        atrasados++;
                    }
                }
            }

            cartaoObras.setValor(obras, copias + " cópias no acervo");
            cartaoDisponiveis.setValor(disponiveis, "prontas para empréstimo");
            cartaoAtivos.setValor(ativos, "ainda não devolvidos");
            cartaoAtrasados.setValor(atrasados, atrasados == 0 ? "nenhum atraso" : "precisam de atenção");
            cartaoAtrasados.setCorDestaque(atrasados == 0 ? Tema.VERDE : Tema.VERMELHO);

        // >>> TRATAMENTO DE EXCEÇÕES NA UI
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Não foi possível carregar os indicadores:\n" + e.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}