package br.com.biblioteca.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class ConexaoFactory {

    private static final String URL = "jdbc:sqlite:biblioteca.db";

    // construtor privado: ninguém instancia esta classe
    private ConexaoFactory() {
    }

    public static Connection getConexao() throws SQLException {
        Connection conexao = DriverManager.getConnection(URL);
        try (Statement st = conexao.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conexao;
    }
}