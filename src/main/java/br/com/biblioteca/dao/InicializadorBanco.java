package br.com.biblioteca.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class InicializadorBanco {

    private static final String[] TABELAS = {
        """
        CREATE TABLE IF NOT EXISTS leitor (
            id        INTEGER PRIMARY KEY AUTOINCREMENT,
            nome      TEXT NOT NULL,
            cpf       TEXT NOT NULL UNIQUE,
            email     TEXT,
            telefone  TEXT,
            matricula TEXT NOT NULL UNIQUE,
            tipo      TEXT NOT NULL CHECK (tipo IN ('ALUNO', 'PROFESSOR'))
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS funcionario (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            nome       TEXT NOT NULL,
            cpf        TEXT NOT NULL UNIQUE,
            email      TEXT,
            telefone   TEXT,
            cargo      TEXT,
            login      TEXT NOT NULL UNIQUE,
            senha_hash TEXT NOT NULL,
            salt       TEXT NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS obra (
            id      INTEGER PRIMARY KEY AUTOINCREMENT,
            titulo  TEXT NOT NULL,
            autor   TEXT NOT NULL,
            editora TEXT,
            ano     INTEGER,
            isbn    TEXT UNIQUE
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS copia (
            id       INTEGER PRIMARY KEY AUTOINCREMENT,
            codigo   TEXT NOT NULL UNIQUE,
            situacao TEXT NOT NULL,
            obra_id  INTEGER NOT NULL REFERENCES obra(id)
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS emprestimo (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            leitor_id       INTEGER NOT NULL REFERENCES leitor(id),
            copia_id        INTEGER NOT NULL REFERENCES copia(id),
            funcionario_id  INTEGER NOT NULL REFERENCES funcionario(id),
            data_emprestimo TEXT NOT NULL,
            data_prevista   TEXT NOT NULL,
            data_devolucao  TEXT,
            multa           TEXT NOT NULL DEFAULT '0.00'
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS reserva (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            leitor_id    INTEGER NOT NULL REFERENCES leitor(id),
            obra_id      INTEGER NOT NULL REFERENCES obra(id),
            data_reserva TEXT NOT NULL,
            situacao     TEXT NOT NULL
        )
        """
    };

    private InicializadorBanco() {
    }

    public static void criarTabelas() throws SQLException {
        try (Connection conexao = ConexaoFactory.getConexao();
             Statement st = conexao.createStatement()) {
            for (String sql : TABELAS) {
                st.execute(sql);
            }
        }
    }
}
