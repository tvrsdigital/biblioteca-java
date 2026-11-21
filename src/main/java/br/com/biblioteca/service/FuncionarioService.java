package br.com.biblioteca.service;

import java.sql.SQLException;

import br.com.biblioteca.dao.FuncionarioDAO;
import br.com.biblioteca.model.Funcionario;
import br.com.biblioteca.util.SenhaUtil;

public class FuncionarioService {

    private final FuncionarioDAO funcionarioDAO = new FuncionarioDAO();

    /**
     * Garante que exista ao menos um funcionário para o primeiro login.
     * Chamado ao iniciar o programa: num banco novo, cria admin / admin123.
     */
    public void garantirAdministrador() throws SQLException {
        if (funcionarioDAO.listarTodos().isEmpty()) {
            String salt = SenhaUtil.gerarSalt();
            funcionarioDAO.salvar(new Funcionario("Administrador", "000.000.000-00",
                "admin@biblioteca.com", "", "Bibliotecário", "admin",
                SenhaUtil.gerarHash("admin123", salt), salt));
        }
    }
}