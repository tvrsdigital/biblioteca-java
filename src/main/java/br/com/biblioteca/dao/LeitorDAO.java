package br.com.biblioteca.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.com.biblioteca.model.Aluno;
import br.com.biblioteca.model.Leitor;
import br.com.biblioteca.model.Professor;

/**
 * DAO (Data Access Object) de Leitor.
 * Responsável por traduzir objetos Leitor (Aluno/Professor) em linhas da
 * tabela "leitor" e vice-versa. Todo o SQL de leitor fica concentrado aqui.
 *
 * Os métodos declaram "throws SQLException": o DAO não trata erros de banco,
 * apenas os repassa. Quem decide o que mostrar ao usuário é a camada de UI.
 */
public class LeitorDAO {

    /**
     * Grava um leitor novo no banco e preenche o id gerado no próprio objeto.
     */
    public void salvar(Leitor leitor) throws SQLException {
        // INSERT INTO leitor (...)   -> insere uma linha na tabela "leitor",
        //                               preenchendo as colunas listadas
        // VALUES (?, ?, ...)         -> os valores entram depois, um para cada "?"
        // RETURNING id               -> pede ao banco que devolva o id que ele gerou
        String sql = """
            INSERT INTO leitor (nome, cpf, email, telefone, matricula, tipo)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        // try-with-resources: a conexão e o comando são fechados
        // automaticamente ao sair do bloco, mesmo se ocorrer um erro.
        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            // Preenche cada "?" na ordem em que aparecem (a contagem começa em 1).
            // Usar "?" em vez de concatenar texto impede SQL Injection.
            ps.setString(1, leitor.getNome());
            ps.setString(2, leitor.getCpf());
            ps.setString(3, leitor.getEmail());
            ps.setString(4, leitor.getTelefone());
            ps.setString(5, leitor.getMatricula());
            ps.setString(6, leitor.getTipo());   // polimorfismo: "ALUNO" ou "PROFESSOR"

            // Por causa do RETURNING, o INSERT devolve uma linha com o id,
            // então usamos executeQuery (o método dos comandos que devolvem linhas).
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {                       // vai para a linha devolvida
                    leitor.setId(rs.getInt("id"));     // o objeto deixa de ter id null
                }
            }
        }
    }

    /**
     * Busca um leitor pelo id. Devolve null se não existir.
     */
    public Leitor buscarPorId(int id) throws SQLException {
        // SELECT *          -> traga todas as colunas
        // FROM leitor       -> da tabela "leitor"
        // WHERE id = ?      -> só a linha cujo id for igual ao informado
        String sql = "SELECT * FROM leitor WHERE id = ?";

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setInt(1, id);   // o único "?" recebe o id procurado

            // ResultSet = o resultado da consulta, lido linha por linha
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {              // existe uma linha? (no máximo uma, o id é único)
                    return montarLeitor(rs);  // converte a linha em objeto
                }
                return null;                  // nenhuma linha: leitor não encontrado
            }
        }
    }

    /**
     * Lista todos os leitores, em ordem alfabética de nome.
     */
    public List<Leitor> listarTodos() throws SQLException {
        // ORDER BY nome -> ordena o resultado pelo nome (A a Z)
        String sql = "SELECT * FROM leitor ORDER BY nome";

        List<Leitor> leitores = new ArrayList<>();   // começa vazia

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {     // sem "?", já executa direto

            // while em vez de if: agora podem vir várias linhas.
            // rs.next() avança para a próxima e devolve false quando acabarem.
            while (rs.next()) {
                leitores.add(montarLeitor(rs));
            }
        }
        return leitores;   // se a tabela estiver vazia, devolve lista vazia (nunca null)
    }

    /**
     * Atualiza os dados de um leitor que já existe no banco.
     */
    public void atualizar(Leitor leitor) throws SQLException {
        // UPDATE leitor          -> altera linhas da tabela "leitor"
        // SET coluna = ?, ...    -> novos valores de cada coluna
        // WHERE id = ?           -> SÓ a linha com este id
        //                           (sem WHERE, alteraria TODOS os leitores!)
        String sql = """
            UPDATE leitor
            SET nome = ?, cpf = ?, email = ?, telefone = ?, matricula = ?, tipo = ?
            WHERE id = ?
            """;

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, leitor.getNome());
            ps.setString(2, leitor.getCpf());
            ps.setString(3, leitor.getEmail());
            ps.setString(4, leitor.getTelefone());
            ps.setString(5, leitor.getMatricula());
            ps.setString(6, leitor.getTipo());
            ps.setInt(7, leitor.getId());   // o 7º "?" é o do WHERE

            // executeUpdate: para comandos que alteram dados e não devolvem linhas
            ps.executeUpdate();
        }
    }

    /**
     * Remove um leitor pelo id.
     */
    public void excluir(int id) throws SQLException {
        // DELETE FROM leitor WHERE id = ? -> apaga só a linha com este id
        String sql = "DELETE FROM leitor WHERE id = ?";

        try (Connection conexao = ConexaoFactory.getConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Converte a linha atual do ResultSet em um objeto Aluno ou Professor.
     * É private porque é um detalhe interno do DAO, usado por buscarPorId e listarTodos.
     */
    private Leitor montarLeitor(ResultSet rs) throws SQLException {
        // Lê cada coluna da linha atual pelo nome da coluna
        int id = rs.getInt("id");
        String nome = rs.getString("nome");
        String cpf = rs.getString("cpf");
        String email = rs.getString("email");
        String telefone = rs.getString("telefone");
        String matricula = rs.getString("matricula");
        String tipo = rs.getString("tipo");

        // Para CRIAR um objeto é preciso escolher a classe concreta
        // (não existe "new Leitor", pois Leitor é abstrata).
        // Este é o ÚNICO ponto do sistema que distingue os tipos de leitor;
        // no resto do código, o polimorfismo cuida das diferenças.
        return switch (tipo) {
            case "ALUNO" -> new Aluno(id, nome, cpf, email, telefone, matricula);
            case "PROFESSOR" -> new Professor(id, nome, cpf, email, telefone, matricula);
            default -> throw new IllegalStateException("Tipo de leitor desconhecido: " + tipo);
        };
    }
}