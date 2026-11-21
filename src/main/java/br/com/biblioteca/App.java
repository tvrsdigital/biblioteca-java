package br.com.biblioteca;

import java.sql.Connection;
import java.sql.SQLException;

import br.com.biblioteca.dao.ConexaoFactory;

public class App {
    public static void main(String[] args) {
        try (Connection conexao = ConexaoFactory.getConexao()) {
            System.out.println("Conectado ao SQLite "
                + conexao.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            System.out.println("Erro ao conectar: " + e.getMessage());
        }
    }
}
