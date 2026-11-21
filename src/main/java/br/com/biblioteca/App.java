package br.com.biblioteca;

import java.sql.SQLException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import br.com.biblioteca.dao.InicializadorBanco;
import br.com.biblioteca.service.FuncionarioService;
import br.com.biblioteca.view.LoginFrame;
import br.com.biblioteca.view.Tema;

public class App {
    public static void main(String[] args) {
        Tema.aplicar(false);   // tema claro ao iniciar

        // Prepara o banco: cria as tabelas e o admin, se necessário
        try {
            InicializadorBanco.criarTabelas();
            new FuncionarioService().garantirAdministrador();
        // >>> TRATAMENTO DE EXCEÇÕES NA UI
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                "Não foi possível preparar o banco de dados:\n" + e.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Toda criação e alteração de tela deve acontecer na thread do Swing (EDT).
        // invokeLater agenda a abertura da tela de login nessa thread.
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}