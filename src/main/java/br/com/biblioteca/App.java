package br.com.biblioteca;

import java.sql.SQLException;
import java.time.LocalDate;

import br.com.biblioteca.dao.CopiaDAO;
import br.com.biblioteca.dao.EmprestimoDAO;
import br.com.biblioteca.dao.FuncionarioDAO;
import br.com.biblioteca.dao.InicializadorBanco;
import br.com.biblioteca.dao.LeitorDAO;
import br.com.biblioteca.dao.ReservaDAO;
import br.com.biblioteca.model.Copia;
import br.com.biblioteca.model.Emprestimo;
import br.com.biblioteca.model.Funcionario;
import br.com.biblioteca.model.Leitor;
import br.com.biblioteca.model.Reserva;

public class App {
    public static void main(String[] args) {
        try {
            InicializadorBanco.criarTabelas();

            LeitorDAO leitorDAO = new LeitorDAO();
            CopiaDAO copiaDAO = new CopiaDAO();
            FuncionarioDAO funcionarioDAO = new FuncionarioDAO();
            EmprestimoDAO emprestimoDAO = new EmprestimoDAO();
            ReservaDAO reservaDAO = new ReservaDAO();

            Leitor ana = leitorDAO.buscarPorId(1);
            Leitor carlos = leitorDAO.buscarPorId(2);
            Funcionario admin = funcionarioDAO.buscarPorLogin("admin");
            Copia c2 = copiaDAO.buscarPorId(2);

            // Só cria na primeira execução
            if (emprestimoDAO.listarTodos().isEmpty()) {
                Emprestimo e = new Emprestimo(ana, c2, admin, LocalDate.of(2026, 11, 1));
                emprestimoDAO.salvar(e);
                System.out.println("Empréstimo registrado com id " + e.getId());

                Reserva r = new Reserva(carlos, c2.getObra(), LocalDate.now());
                reservaDAO.salvar(r);
                System.out.println("Reserva registrada com id " + r.getId());
            }

            // Lê de volta: os objetos associados chegam completos
            for (Emprestimo e : emprestimoDAO.listarTodos()) {
                System.out.println("Empréstimo " + e.getId() + ": " + e.getCopia()
                    + " para " + e.getLeitor() + " (" + e.getLeitor().getTipo() + ")"
                    + ", por " + e.getFuncionario()
                    + ", devolver até " + e.getDataPrevista());
                System.out.println("   Atrasado hoje? " + e.estaAtrasado(LocalDate.now())
                    + " | multa até hoje: R$ " + e.calcularMulta(LocalDate.now()));
            }

            System.out.println("Ativos da Ana: " + emprestimoDAO.listarAtivosPorLeitor(ana.getId()).size());

            for (Reserva r : reservaDAO.listarAtivasPorObra(c2.getObra().getId())) {
                System.out.println("Fila de reserva: " + r.getLeitor() + " aguarda " + r.getObra());
            }

        } catch (SQLException e) {
            System.out.println("Erro no banco de dados: " + e.getMessage());
        }
    }
}