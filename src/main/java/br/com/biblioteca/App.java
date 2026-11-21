package br.com.biblioteca;

import java.sql.SQLException;

import br.com.biblioteca.dao.FuncionarioDAO;
import br.com.biblioteca.dao.InicializadorBanco;
import br.com.biblioteca.model.Funcionario;
import br.com.biblioteca.util.SenhaUtil;

public class App {
    public static void main(String[] args) {
        try {
            InicializadorBanco.criarTabelas();
            FuncionarioDAO dao = new FuncionarioDAO();
            Funcionario admin = dao.buscarPorLogin("admin");

            // Substitui o hash provisório por um hash real (só na primeira vez)
            if (admin.getSalt().equals("salt-provisorio")) {
                String salt = SenhaUtil.gerarSalt();
                admin.setSalt(salt);
                admin.setSenhaHash(SenhaUtil.gerarHash("admin123", salt));
                dao.atualizar(admin);
                System.out.println("Senha do admin definida com hash real.");
            }

            admin = dao.buscarPorLogin("admin");   // relê do banco
            System.out.println("Salt guardado: " + admin.getSalt());
            System.out.println("Hash guardado: " + admin.getSenhaHash());

            System.out.println("Senha 'admin123' confere? "
                + SenhaUtil.verificar("admin123", admin.getSalt(), admin.getSenhaHash()));
            System.out.println("Senha 'admin124' confere? "
                + SenhaUtil.verificar("admin124", admin.getSalt(), admin.getSenhaHash()));

            // Mesma senha, salts diferentes = hashes diferentes
            String s1 = SenhaUtil.gerarSalt();
            String s2 = SenhaUtil.gerarSalt();
            System.out.println("Mesma senha, hash 1: " + SenhaUtil.gerarHash("123456", s1));
            System.out.println("Mesma senha, hash 2: " + SenhaUtil.gerarHash("123456", s2));

        } catch (SQLException e) {
            System.out.println("Erro no banco de dados: " + e.getMessage());
        }
    }
}