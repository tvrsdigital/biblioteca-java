package br.com.biblioteca;

import java.sql.SQLException;

import br.com.biblioteca.dao.FuncionarioDAO;
import br.com.biblioteca.dao.InicializadorBanco;
import br.com.biblioteca.dao.LeitorDAO;
import br.com.biblioteca.model.Funcionario;
import br.com.biblioteca.model.Leitor;

public class App {
    public static void main(String[] args) {
        try {
            InicializadorBanco.criarTabelas();

            FuncionarioDAO funcionarioDAO = new FuncionarioDAO();

            // Só cadastra o admin se ele ainda não existir
            if (funcionarioDAO.buscarPorLogin("admin") == null) {
                // "hash-provisorio" e "salt-provisorio" são temporários:
                // o hash de verdade será criado no passo das senhas.
                Funcionario admin = new Funcionario("Administrador", "000.000.000-00",
                    "admin@biblioteca.com", "0000000", "Bibliotecário",
                    "admin", "hash-provisorio", "salt-provisorio");
                funcionarioDAO.salvar(admin);
                System.out.println("Admin cadastrado com id " + admin.getId());
            } else {
                System.out.println("Admin já existia, nada a fazer.");
            }

            // Busca pelo login e mostra os dados
            Funcionario encontrado = funcionarioDAO.buscarPorLogin("admin");
            System.out.println("Encontrado: " + encontrado + " - " + encontrado.getCargo());

            // Busca um login que não existe
            System.out.println("Login 'fulano': " + funcionarioDAO.buscarPorLogin("fulano"));

            // Os leitores do teste anterior continuam no banco
            for (Leitor l : new LeitorDAO().listarTodos()) {
                System.out.println("Leitor: " + l + " (" + l.getTipo() + ")");
            }

        } catch (SQLException e) {
            System.out.println("Erro no banco de dados: " + e.getMessage());
        }
    }
}