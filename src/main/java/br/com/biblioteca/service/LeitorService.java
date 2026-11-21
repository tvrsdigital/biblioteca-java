package br.com.biblioteca.service;

import java.sql.SQLException;
import java.util.List;

import br.com.biblioteca.dao.EmprestimoDAO;
import br.com.biblioteca.dao.LeitorDAO;
import br.com.biblioteca.exception.NegocioException;
import br.com.biblioteca.model.Leitor;

/** Cadastro de leitores, com validações e verificação de duplicidade. */
public class LeitorService {

    private final LeitorDAO leitorDAO = new LeitorDAO();
    private final EmprestimoDAO emprestimoDAO = new EmprestimoDAO();

    /**
     * Salva um leitor: se não tem id, é novo (INSERT); se tem, já existe (UPDATE).
     * A tela chama sempre este método, sem precisar decidir.
     */
    public void salvar(Leitor leitor) throws NegocioException, SQLException {
        validarCampos(leitor);
        verificarDuplicidade(leitor);

        if (leitor.getId() == null) {
            leitorDAO.salvar(leitor);
        } else {
            leitorDAO.atualizar(leitor);
        }
    }

    public void excluir(Leitor leitor) throws NegocioException, SQLException {
        if (!emprestimoDAO.listarAtivosPorLeitor(leitor.getId()).isEmpty()) {
            throw new NegocioException(leitor.getNome() + " possui empréstimos ativos e não pode ser excluído.");
        }
        try {
            leitorDAO.excluir(leitor.getId());
        } catch (SQLException e) {
            // Chave estrangeira: o leitor tem histórico de empréstimos ou reservas.
            // Traduzimos o erro técnico do banco em uma mensagem de negócio.
            if (e.getMessage() != null && e.getMessage().contains("FOREIGN KEY")) {
                throw new NegocioException(leitor.getNome()
                    + " possui histórico de empréstimos ou reservas e não pode ser excluído.");
            }
            throw e;   // qualquer outro erro de banco segue como erro técnico
        }
    }

    public List<Leitor> listarTodos() throws SQLException {
        return leitorDAO.listarTodos();
    }

    // ----- validações (private: detalhes internos do service) -----

    private void validarCampos(Leitor leitor) throws NegocioException {
        if (estaVazio(leitor.getNome())) {
            throw new NegocioException("O nome é obrigatório.");
        }
        if (estaVazio(leitor.getCpf())) {
            throw new NegocioException("O CPF é obrigatório.");
        }
        if (estaVazio(leitor.getMatricula())) {
            throw new NegocioException("A matrícula é obrigatória.");
        }
    }

    private void verificarDuplicidade(Leitor leitor) throws NegocioException, SQLException {
        for (Leitor outro : leitorDAO.listarTodos()) {
            // Ignora o próprio leitor (caso de atualização)
            boolean ehOMesmo = leitor.getId() != null && leitor.getId().equals(outro.getId());
            if (ehOMesmo) {
                continue;   // pula para a próxima volta do laço
            }
            if (outro.getCpf().equals(leitor.getCpf())) {
                throw new NegocioException("Já existe um leitor com o CPF " + leitor.getCpf() + ".");
            }
            if (outro.getMatricula().equals(leitor.getMatricula())) {
                throw new NegocioException("Já existe um leitor com a matrícula " + leitor.getMatricula() + ".");
            }
        }
    }

    private boolean estaVazio(String texto) {
        return texto == null || texto.isBlank();
    }
}