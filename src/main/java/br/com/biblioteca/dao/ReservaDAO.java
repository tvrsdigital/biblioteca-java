package br.com.biblioteca.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.biblioteca.model.Leitor;
import br.com.biblioteca.model.Obra;
import br.com.biblioteca.model.Reserva;
import br.com.biblioteca.model.SituacaoReserva;

/**
 * DAO de Reserva. Mesmo padrão do EmprestimoDAO:
 * a reserva aponta para um leitor e uma OBRA (não uma cópia).
 */
public class ReservaDAO {

    private final LeitorDAO leitorDAO = new LeitorDAO();
    private final ObraDAO obraDAO = new ObraDAO();

    public void salvar(Reserva r) throws SQLException {
        String sql = """
            INSERT INTO reserva (leitor_id, obra_id, data_reserva, situacao)
            VALUES (?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setInt(1, r.getLeitor().getId());
            ps.setInt(2, r.getObra().getId());
            ps.setString(3, r.getDataReserva().toString());
            ps.setString(4, r.getSituacao().name());   // enum -> texto

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    r.setId(rs.getInt("id"));
                }
            }
        }
    }

    /** Grava a nova situação, depois de reserva.atender() ou reserva.cancelar(). */
    public void atualizarSituacao(Reserva r) throws SQLException {
        String sql = "UPDATE reserva SET situacao = ? WHERE id = ?";

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, r.getSituacao().name());
            ps.setInt(2, r.getId());
            ps.executeUpdate();
        }
    }

    public Reserva buscarPorId(int id) throws SQLException {
        List<Reserva> resultado = consultar("SELECT * FROM reserva WHERE id = ?", id);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public List<Reserva> listarTodas() throws SQLException {
        return consultar("SELECT * FROM reserva ORDER BY data_reserva DESC, id DESC");
    }

    /**
     * Reservas ATIVAS de uma obra, da mais antiga para a mais nova.
     * É a "fila" da RN06: a primeira da lista é a próxima a ser atendida.
     * O id desempata reservas feitas no mesmo dia (quem reservou primeiro tem id menor).
     */
    public List<Reserva> listarAtivasPorObra(int obraId) throws SQLException {
        return consultar("""
            SELECT * FROM reserva
            WHERE obra_id = ? AND situacao = 'ATIVA'
            ORDER BY data_reserva, id
            """, obraId);
    }

    private List<Reserva> consultar(String sql, Integer... parametros) throws SQLException {
        List<Reserva> lista = new ArrayList<>();

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            for (int i = 0; i < parametros.length; i++) {
                ps.setInt(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montarReserva(rs));
                }
            }
        }
        return lista;
    }

    private Reserva montarReserva(ResultSet rs) throws SQLException {
        Leitor leitor = leitorDAO.buscarPorId(rs.getInt("leitor_id"));
        Obra obra = obraDAO.buscarPorId(rs.getInt("obra_id"));

        return new Reserva(
            rs.getInt("id"),
            leitor,
            obra,
            LocalDate.parse(rs.getString("data_reserva")),
            SituacaoReserva.valueOf(rs.getString("situacao"))   // texto -> enum
        );
    }
}