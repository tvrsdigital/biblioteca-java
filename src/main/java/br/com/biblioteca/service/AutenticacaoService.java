package br.com.biblioteca.service;

import java.sql.SQLException;

import br.com.biblioteca.dao.FuncionarioDAO;
import br.com.biblioteca.exception.NegocioException;
import br.com.biblioteca.model.Funcionario;
import br.com.biblioteca.util.SenhaUtil;

/**
 * Autenticação de funcionários (RN08).
 * Declara as duas exceções: NegocioException (regra violada: login inválido)
 * e SQLException (falha técnica do banco). Quem trata as duas é a tela.
 */
public class AutenticacaoService {

    private final FuncionarioDAO funcionarioDAO = new FuncionarioDAO();

    public Funcionario autenticar(String login, String senha) throws NegocioException, SQLException {
        // Validação básica: campos vazios nem chegam ao banco
        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            throw new NegocioException("Informe o login e a senha.");
        }

        Funcionario funcionario = funcionarioDAO.buscarPorLogin(login.trim());

        // Mesma mensagem para "login não existe" e "senha errada":
        // dizer qual dos dois falhou ajudaria alguém a adivinhar logins válidos.
        if (funcionario == null
                || !SenhaUtil.verificar(senha, funcionario.getSalt(), funcionario.getSenhaHash())) {
            throw new NegocioException("Login ou senha inválidos.");
        }

        return funcionario;   // a tela guarda este objeto como "funcionário logado" (RN09)
    }
}