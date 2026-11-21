package br.com.biblioteca.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.com.biblioteca.model.Funcionario;

/**
 * DAO de Funcionario. Mesmo padrão do LeitorDAO, com duas diferenças:
 *  - não há tipos diferentes (Funcionario é uma classe concreta, sem subclasses),
 *    então o montarFuncionario não precisa de switch;
 *  - existe a busca por login, usada na tela de entrada do sistema.
 */
public class FuncionarioDAO {

    public void salvar(Funcionario f) throws SQLException {
        // Repare: gravamos senha_hash e salt. A senha digitada NUNCA chega ao banco.
        String sql = """
            INSERT INTO funcionario (nome, cpf, email, telefone, cargo, login, senha_hash, salt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, f.getNome());
            ps.setString(2, f.getCpf());
            ps.setString(3, f.getEmail());
            ps.setString(4, f.getTelefone());
            ps.setString(5, f.getCargo());
            ps.setString(6, f.getLogin());
            ps.setString(7, f.getSenhaHash());
            ps.setString(8, f.getSalt());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    f.setId(rs.getInt("id"));
                }
            }
        }
    }

    public Funcionario buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM funcionario WHERE id = ?";

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return montarFuncionario(rs);
                }
                return null;
            }
        }
    }

    /**
     * NOVO: busca pelo login (usado na autenticação).
     * Funciona igual ao buscarPorId, só muda a coluna do WHERE.
     * Como login é UNIQUE no banco, vem no máximo uma linha.
     */
    public Funcionario buscarPorLogin(String login) throws SQLException {
        String sql = "SELECT * FROM funcionario WHERE login = ?";

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, login);   // setString, pois login é texto
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return montarFuncionario(rs);
                }
                return null;          // login inexistente
            }
        }
    }

    public List<Funcionario> listarTodos() throws SQLException {
        String sql = "SELECT * FROM funcionario ORDER BY nome";
        List<Funcionario> funcionarios = new ArrayList<>();

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                funcionarios.add(montarFuncionario(rs));
            }
        }
        return funcionarios;
    }

    public void atualizar(Funcionario f) throws SQLException {
        String sql = """
            UPDATE funcionario
            SET nome = ?, cpf = ?, email = ?, telefone = ?, cargo = ?,
                login = ?, senha_hash = ?, salt = ?
            WHERE id = ?
            """;

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, f.getNome());
            ps.setString(2, f.getCpf());
            ps.setString(3, f.getEmail());
            ps.setString(4, f.getTelefone());
            ps.setString(5, f.getCargo());
            ps.setString(6, f.getLogin());
            ps.setString(7, f.getSenhaHash());
            ps.setString(8, f.getSalt());
            ps.setInt(9, f.getId());   // o último "?" é o do WHERE

            ps.executeUpdate();
        }
    }

    public void excluir(int id) throws SQLException {
        String sql = "DELETE FROM funcionario WHERE id = ?";

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // Sem switch: só existe um tipo de funcionário,
    // então sempre usamos o construtor "do banco" (com id).
    private Funcionario montarFuncionario(ResultSet rs) throws SQLException {
        return new Funcionario(
            rs.getInt("id"),
            rs.getString("nome"),
            rs.getString("cpf"),
            rs.getString("email"),
            rs.getString("telefone"),
            rs.getString("cargo"),
            rs.getString("login"),
            rs.getString("senha_hash"),   // nome da coluna no banco (snake_case)
            rs.getString("salt")
        );
    }
}