package br.com.biblioteca.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.biblioteca.model.Copia;
import br.com.biblioteca.model.Emprestimo;
import br.com.biblioteca.model.Funcionario;
import br.com.biblioteca.model.Leitor;

/**
 * DAO de Emprestimo.
 * No banco, as associações viram ids (leitor_id, copia_id, funcionario_id).
 * Ao ler, os objetos completos são remontados com os outros DAOs.
 */
public class EmprestimoDAO {

    private final LeitorDAO leitorDAO = new LeitorDAO();
    private final CopiaDAO copiaDAO = new CopiaDAO();
    private final FuncionarioDAO funcionarioDAO = new FuncionarioDAO();

    public void salvar(Emprestimo e) throws SQLException {
        String sql = """
            INSERT INTO emprestimo (leitor_id, copia_id, funcionario_id,
                                    data_emprestimo, data_prevista, data_devolucao, multa)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            // Associações -> chaves estrangeiras: gravamos só os ids
            ps.setInt(1, e.getLeitor().getId());
            ps.setInt(2, e.getCopia().getId());
            ps.setInt(3, e.getFuncionario().getId());

            // LocalDate -> texto no formato ISO ("2026-11-21")
            ps.setString(4, e.getDataEmprestimo().toString());
            ps.setString(5, e.getDataPrevista().toString());
            ps.setString(6, paraTexto(e.getDataDevolucao()));   // pode ser null

            // BigDecimal -> texto ("0.00"); toPlainString evita notação científica
            ps.setString(7, e.getMulta().toPlainString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    e.setId(rs.getInt("id"));
                }
            }
        }
    }

    /**
     * Grava a devolução: data de devolução e multa final.
     * Usado depois de emprestimo.registrarDevolucao(data).
     */
    public void atualizarDevolucao(Emprestimo e) throws SQLException {
        String sql = "UPDATE emprestimo SET data_devolucao = ?, multa = ? WHERE id = ?";

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, paraTexto(e.getDataDevolucao()));
            ps.setString(2, e.getMulta().toPlainString());
            ps.setInt(3, e.getId());
            ps.executeUpdate();
        }
    }

    public Emprestimo buscarPorId(int id) throws SQLException {
        List<Emprestimo> resultado = consultar("SELECT * FROM emprestimo WHERE id = ?", id);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    /** Todos os empréstimos, do mais recente para o mais antigo (DESC = decrescente). */
    public List<Emprestimo> listarTodos() throws SQLException {
        return consultar("SELECT * FROM emprestimo ORDER BY data_emprestimo DESC, id DESC");
    }

    /**
     * Empréstimos ainda não devolvidos de um leitor.
     * "data_devolucao IS NULL" = ainda não devolvido (em SQL, nulo se testa com IS NULL).
     * Usado nas regras de limite (RN01/RN02) e de atraso (RN04).
     */
    public List<Emprestimo> listarAtivosPorLeitor(int leitorId) throws SQLException {
        return consultar("""
            SELECT * FROM emprestimo
            WHERE leitor_id = ? AND data_devolucao IS NULL
            ORDER BY data_prevista
            """, leitorId);
    }

    /**
     * O empréstimo ativo de uma cópia (no máximo um: uma cópia
     * não pode estar emprestada duas vezes ao mesmo tempo).
     * Usado na devolução.
     */
    public Emprestimo buscarAtivoPorCopia(int copiaId) throws SQLException {
        List<Emprestimo> resultado = consultar("""
            SELECT * FROM emprestimo
            WHERE copia_id = ? AND data_devolucao IS NULL
            """, copiaId);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    /**
     * Método auxiliar que executa qualquer SELECT de empréstimos.
     * Os métodos públicos acima só informam o SQL e o parâmetro.
     * "Integer... parametros" (varargs) aceita zero, um ou vários valores.
     */
    private List<Emprestimo> consultar(String sql, Integer... parametros) throws SQLException {
        List<Emprestimo> lista = new ArrayList<>();

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            for (int i = 0; i < parametros.length; i++) {
                ps.setInt(i + 1, parametros[i]);   // os "?" começam em 1
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montarEmprestimo(rs));
                }
            }
        }
        return lista;
    }

    private Emprestimo montarEmprestimo(ResultSet rs) throws SQLException {
        // Remonta as associações buscando cada objeto pelo seu DAO
        Leitor leitor = leitorDAO.buscarPorId(rs.getInt("leitor_id"));
        Copia copia = copiaDAO.buscarPorId(rs.getInt("copia_id"));
        Funcionario funcionario = funcionarioDAO.buscarPorId(rs.getInt("funcionario_id"));

        return new Emprestimo(
            rs.getInt("id"),
            leitor,
            copia,
            funcionario,
            LocalDate.parse(rs.getString("data_emprestimo")),   // texto -> LocalDate
            LocalDate.parse(rs.getString("data_prevista")),
            paraData(rs.getString("data_devolucao")),            // pode ser null
            new BigDecimal(rs.getString("multa"))                // texto -> BigDecimal (exato)
        );
    }

    // Conversões que aceitam null (empréstimo ainda não devolvido)
    private String paraTexto(LocalDate data) {
        return data == null ? null : data.toString();
    }

    private LocalDate paraData(String texto) {
        return texto == null ? null : LocalDate.parse(texto);
    }
}