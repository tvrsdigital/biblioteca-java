package br.com.biblioteca;

import java.sql.Connection;
import java.sql.SQLException;

import br.com.biblioteca.dao.ConexaoFactory;
import br.com.biblioteca.dao.InicializadorBanco;
import br.com.biblioteca.dao.LeitorDAO;
import br.com.biblioteca.model.Aluno;
import br.com.biblioteca.model.Leitor;
import br.com.biblioteca.model.Professor;

public class App {
    public static void main(String[] args) {
        // O main faz o papel da "UI" por enquanto: é ele que CAPTURA
        // as exceções e mostra uma mensagem ao usuário.
        try (Connection conexao = ConexaoFactory.getConexao()) {
            System.out.println("Conectado ao SQLite "
                + conexao.getMetaData().getDatabaseProductVersion());

            // Cria as tabelas (só na primeira vez; depois não faz nada)
            InicializadorBanco.criarTabelas();
            System.out.println("Tabelas criadas/verificadas.");

            LeitorDAO dao = new LeitorDAO();

            // Salva uma aluna e mostra o id que o banco gerou
            Aluno ana = new Aluno("Ana", "111.111.111-11", "ana@email.com", "1199999", "A001");
            dao.salvar(ana);
            System.out.println("Ana salva com id " + ana.getId());

            // Salva um professor
            dao.salvar(new Professor("Carlos", "222.222.222-22", "carlos@email.com", "1188888", "P001"));

            // Lê tudo de volta do banco: os objetos voltam como Aluno e Professor,
            // e o polimorfismo continua funcionando (prazo 7 e 14)
            for (Leitor l : dao.listarTodos()) {
                System.out.println(l.getId() + " - " + l + " (" + l.getTipo()
                    + "), prazo " + l.getPrazoDias());
            }

        } catch (SQLException e) {
            System.out.println("Erro no banco de dados: " + e.getMessage());
        }
    }
}