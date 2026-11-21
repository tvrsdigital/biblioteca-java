package br.com.biblioteca.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import br.com.biblioteca.model.Copia;
import br.com.biblioteca.model.Obra;
import br.com.biblioteca.model.SituacaoCopia;

/**
 * DAO de Copia.
 * Uma cópia sempre pertence a uma obra: no banco, isso é a coluna obra_id.
 */
public class CopiaDAO {

    /**
     * Grava uma cópia nova.
     * Pré-condição: a cópia já deve ter sido adicionada a uma obra SALVA
     * (obra.adicionarCopia(copia)), pois precisamos do id da obra.
     */
    public void salvar(Copia copia) throws SQLException {
        String sql = """
            INSERT INTO copia (codigo, situacao, obra_id)
            VALUES (?, ?, ?)
            RETURNING id
            """;

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, copia.getCodigo());
            ps.setString(2, copia.getSituacao().name());   // enum -> texto: "DISPONIVEL"
            ps.setInt(3, copia.getObra().getId());         // a associação vira chave estrangeira

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    copia.setId(rs.getInt("id"));
                }
            }
        }
    }

    /**
     * Atualiza código e situação de uma cópia existente.
     * É o método que será usado ao emprestar, devolver e reservar
     * (a situação muda de DISPONIVEL para EMPRESTADA, por exemplo).
     */
    public void atualizar(Copia copia) throws SQLException {
        String sql = "UPDATE copia SET codigo = ?, situacao = ? WHERE id = ?";

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, copia.getCodigo());
            ps.setString(2, copia.getSituacao().name());
            ps.setInt(3, copia.getId());
            ps.executeUpdate();
        }
    }

    public void excluir(int id) throws SQLException {
        String sql = "DELETE FROM copia WHERE id = ?";

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Carrega do banco todas as cópias de uma obra e as adiciona a ela.
     * É chamado pelo ObraDAO ao montar uma obra.
     */
    public void carregarCopias(Obra obra) throws SQLException {
        // Todas as cópias cujo obra_id é o id desta obra, em ordem de código
        String sql = "SELECT * FROM copia WHERE obra_id = ? ORDER BY codigo";

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setInt(1, obra.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Copia copia = new Copia(
                        rs.getInt("id"),
                        rs.getString("codigo"),
                        SituacaoCopia.valueOf(rs.getString("situacao"))   // texto -> enum
                    );
                    // Pela "porta oficial": entra na lista E recebe a referência à obra
                    obra.adicionarCopia(copia);
                }
            }
        }
    }

    /**
     * Busca uma cópia pelo id, já ligada à sua obra completa.
     * Estratégia: descobre o obra_id da cópia, carrega a obra (com todas
     * as cópias) pelo ObraDAO e devolve, dentro dela, a cópia procurada.
     */
    public Copia buscarPorId(int id) throws SQLException {
        String sql = "SELECT obra_id FROM copia WHERE id = ?";
        int obraId;

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;   // cópia não existe
                }
                obraId = rs.getInt("obra_id");
            }
        }

        // ATENÇÃO: o ObraDAO é criado aqui dentro, e não como atributo da classe.
        // ObraDAO usa CopiaDAO e CopiaDAO usa ObraDAO; se cada um criasse o
        // outro como atributo, um construiria o outro infinitamente (StackOverflowError).
        Obra obra = new ObraDAO().buscarPorId(obraId);

        // Procura, entre as cópias da obra, a que tem o id pedido
        for (Copia copia : obra.getCopias()) {
            if (copia.getId() == id) {
                return copia;
            }
        }
        return null;
    }
}