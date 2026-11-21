package br.com.biblioteca.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import br.com.biblioteca.dao.CopiaDAO;
import br.com.biblioteca.dao.ObraDAO;
import br.com.biblioteca.dao.ReservaDAO;
import br.com.biblioteca.exception.NegocioException;
import br.com.biblioteca.model.Copia;
import br.com.biblioteca.model.Leitor;
import br.com.biblioteca.model.Obra;
import br.com.biblioteca.model.Reserva;
import br.com.biblioteca.model.SituacaoCopia;

public class ReservaService {

    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final CopiaDAO copiaDAO = new CopiaDAO();
    private final ObraDAO obraDAO = new ObraDAO();

    public Reserva reservar(Leitor leitor, Obra obra, LocalDate hoje)
            throws NegocioException, SQLException {

        // RN05: com cópia disponível, faz-se empréstimo, não reserva
        if (obra.temCopiaDisponivel()) {
            throw new NegocioException("\"" + obra.getTitulo()
                + "\" tem cópia disponível. Realize o empréstimo em vez da reserva.");
        }

        // Um leitor não entra duas vezes na mesma fila
        for (Reserva r : reservaDAO.listarAtivasPorObra(obra.getId())) {
            if (r.getLeitor().getId().equals(leitor.getId())) {
                throw new NegocioException(leitor.getNome()
                    + " já possui reserva ativa para \"" + obra.getTitulo() + "\".");
            }
        }

        Reserva reserva = new Reserva(leitor, obra, hoje);
        reservaDAO.salvar(reserva);
        return reserva;
    }

    public void cancelar(Reserva reserva) throws NegocioException, SQLException {
        // Confere antes, para dar uma mensagem amigável
        // (o model também se protege, com IllegalStateException)
        if (!reserva.isAtiva()) {
            throw new NegocioException("Só é possível cancelar uma reserva ativa.");
        }
        reserva.cancelar();
        reservaDAO.atualizarSituacao(reserva);

        // RN06: se a fila ficou vazia, uma cópia que estava RESERVADA volta a ficar DISPONIVEL
        int obraId = reserva.getObra().getId();
        if (reservaDAO.listarAtivasPorObra(obraId).isEmpty()) {
            Obra obra = obraDAO.buscarPorId(obraId);   // relê, para ter as situações atuais
            for (Copia copia : obra.getCopias()) {
                if (copia.getSituacao() == SituacaoCopia.RESERVADA) {
                    copia.setSituacao(SituacaoCopia.DISPONIVEL);
                    copiaDAO.atualizar(copia);
                }
            }
        }
    }

    public List<Reserva> listarTodas() throws SQLException {
        return reservaDAO.listarTodas();
    }
}