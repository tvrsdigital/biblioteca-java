package br.com.biblioteca.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import br.com.biblioteca.dao.CopiaDAO;
import br.com.biblioteca.dao.EmprestimoDAO;
import br.com.biblioteca.dao.ReservaDAO;
import br.com.biblioteca.exception.NegocioException;
import br.com.biblioteca.model.Copia;
import br.com.biblioteca.model.Emprestimo;
import br.com.biblioteca.model.Funcionario;
import br.com.biblioteca.model.Leitor;
import br.com.biblioteca.model.Reserva;
import br.com.biblioteca.model.SituacaoCopia;

/**
 * Regras de empréstimo e devolução.
 * Coordena três DAOs (empréstimo, cópia e reserva) e lança NegocioException
 * com mensagens para o usuário quando alguma regra é violada.
 */
public class EmprestimoService {

    private final EmprestimoDAO emprestimoDAO = new EmprestimoDAO();
    private final CopiaDAO copiaDAO = new CopiaDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();

    /**
     * Realiza um empréstimo.
     * @param funcionario o funcionário logado, que fica registrado no empréstimo (RN09)
     * @param hoje a data do empréstimo (parâmetro para facilitar os testes)
     */
    public Emprestimo realizarEmprestimo(Leitor leitor, Copia copia, Funcionario funcionario,
                                         LocalDate hoje) throws NegocioException, SQLException {

        // --- RN03 e RN06: a cópia pode ser emprestada? ---
        Reserva reservaAtendida = null;

        if (copia.getSituacao() == SituacaoCopia.RESERVADA) {
            // Cópia reservada só pode sair para o PRIMEIRO da fila de reservas
            List<Reserva> fila = reservaDAO.listarAtivasPorObra(copia.getObra().getId());
            boolean ehOPrimeiroDaFila = !fila.isEmpty()
                && fila.get(0).getLeitor().getId().equals(leitor.getId());
            if (!ehOPrimeiroDaFila) {
                throw new NegocioException("A cópia " + copia.getCodigo()
                    + " está reservada para outro leitor.");
            }
            reservaAtendida = fila.get(0);

        } else if (!copia.isDisponivel()) {
            throw new NegocioException("A cópia " + copia.getCodigo()
                + " não está disponível (situação: " + copia.getSituacao() + ").");
        }

        // --- RN01 e RN02: limite de empréstimos (POLIMORFISMO: 3 ou 5) ---
        List<Emprestimo> ativos = emprestimoDAO.listarAtivosPorLeitor(leitor.getId());
        if (ativos.size() >= leitor.getLimiteEmprestimos()) {
            throw new NegocioException(leitor.getNome() + " já atingiu o limite de "
                + leitor.getLimiteEmprestimos() + " empréstimos simultâneos.");
        }

        // --- RN04: nenhum empréstimo em atraso ---
        for (Emprestimo e : ativos) {
            if (e.estaAtrasado(hoje)) {
                throw new NegocioException(leitor.getNome()
                    + " possui empréstimo em atraso (cópia " + e.getCopia().getCodigo()
                    + ", prevista para " + e.getDataPrevista() + ").");
            }
        }

        // --- Tudo certo: registra (o prazo polimórfico é calculado no construtor) ---
        Emprestimo emprestimo = new Emprestimo(leitor, copia, funcionario, hoje);
        emprestimoDAO.salvar(emprestimo);

        copia.setSituacao(SituacaoCopia.EMPRESTADA);
        copiaDAO.atualizar(copia);

        if (reservaAtendida != null) {
            reservaAtendida.atender();               // ATIVA -> ATENDIDA
            reservaDAO.atualizarSituacao(reservaAtendida);
        }

        return emprestimo;
    }

    /**
     * Registra a devolução de uma cópia e devolve o empréstimo encerrado,
     * já com a multa calculada (RN07), para a tela exibir.
     */
    public Emprestimo registrarDevolucao(Copia copia, LocalDate hoje)
            throws NegocioException, SQLException {

        Emprestimo emprestimo = emprestimoDAO.buscarAtivoPorCopia(copia.getId());
        if (emprestimo == null) {
            throw new NegocioException("A cópia " + copia.getCodigo()
                + " não possui empréstimo ativo.");
        }

        // RN07: o próprio Emprestimo grava a data e calcula a multa
        emprestimo.registrarDevolucao(hoje);
        emprestimoDAO.atualizarDevolucao(emprestimo);

        // RN06: se houver fila de reservas, a cópia fica guardada para o primeiro
        Copia copiaDevolvida = emprestimo.getCopia();
        List<Reserva> fila = reservaDAO.listarAtivasPorObra(copiaDevolvida.getObra().getId());
        if (fila.isEmpty()) {
            copiaDevolvida.setSituacao(SituacaoCopia.DISPONIVEL);
        } else {
            copiaDevolvida.setSituacao(SituacaoCopia.RESERVADA);
        }
        copiaDAO.atualizar(copiaDevolvida);

        return emprestimo;
    }

    // Consultas repassadas para as telas (a view nunca fala direto com o DAO)
    public List<Emprestimo> listarTodos() throws SQLException {
        return emprestimoDAO.listarTodos();
    }

    public List<Emprestimo> listarAtivosPorLeitor(Leitor leitor) throws SQLException {
        return emprestimoDAO.listarAtivosPorLeitor(leitor.getId());
    }
}