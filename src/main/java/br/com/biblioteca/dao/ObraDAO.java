package br.com.biblioteca.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.com.biblioteca.model.Obra;

/**
 * DAO de Obra.
 * Salva apenas os dados da obra; as cópias são salvas pelo CopiaDAO.
 * Ao LER uma obra, porém, já traz as cópias junto (agregação montada).
 */
public class ObraDAO {

    // Aqui pode ser atributo: o CopiaDAO não cria um ObraDAO no construtor,
    // só dentro do buscarPorId, então não há construção infinita.
    private final CopiaDAO copiaDAO = new CopiaDAO();

    public void salvar(Obra obra) throws SQLException {
        String sql = """
            INSERT INTO obra (titulo, autor, editora, ano, isbn)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, obra.getTitulo());
            ps.setString(2, obra.getAutor());
            ps.setString(3, obra.getEditora());
            ps.setInt(4, obra.getAno());          // setInt, pois ano é número
            ps.setString(5, obra.getIsbn());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    obra.setId(rs.getInt("id"));
                }
            }
        }
    }

    public void atualizar(Obra obra) throws SQLException {
        String sql = """
            UPDATE obra
            SET titulo = ?, autor = ?, editora = ?, ano = ?, isbn = ?
            WHERE id = ?
            """;

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, obra.getTitulo());
            ps.setString(2, obra.getAutor());
            ps.setString(3, obra.getEditora());
            ps.setInt(4, obra.getAno());
            ps.setString(5, obra.getIsbn());
            ps.setInt(6, obra.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Exclui uma obra. Se ela ainda tiver cópias, o banco recusa
     * (chave estrangeira em copia.obra_id) e lança SQLException.
     */
    public void excluir(int id) throws SQLException {
        String sql = "DELETE FROM obra WHERE id = ?";

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Busca uma obra pelo id, já com suas cópias. */
    public Obra buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM obra WHERE id = ?";
        Obra obra = null;

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    obra = montarObra(rs);
                }
            }
        }

        // As cópias são carregadas depois de fechar a primeira conexão
        if (obra != null) {
            copiaDAO.carregarCopias(obra);
        }
        return obra;
    }

    /** Lista todas as obras em ordem de título, cada uma com suas cópias. */
    public List<Obra> listarTodos() throws SQLException {
        String sql = "SELECT * FROM obra ORDER BY titulo";
        List<Obra> obras = new ArrayList<>();

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                obras.add(montarObra(rs));
            }
        }

        for (Obra obra : obras) {
            copiaDAO.carregarCopias(obra);
        }
        return obras;
    }

    // Monta só os dados da obra; a lista de cópias começa vazia
    private Obra montarObra(ResultSet rs) throws SQLException {
        return new Obra(
            rs.getInt("id"),
            rs.getString("titulo"),
            rs.getString("autor"),
            rs.getString("editora"),
            rs.getInt("ano"),
            rs.getString("isbn")
        );
    }
}