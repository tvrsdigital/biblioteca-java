package br.com.biblioteca.service;

import java.sql.SQLException;
import java.util.List;

import br.com.biblioteca.dao.CopiaDAO;
import br.com.biblioteca.dao.ObraDAO;
import br.com.biblioteca.exception.NegocioException;
import br.com.biblioteca.model.Copia;
import br.com.biblioteca.model.Obra;

/** Cadastro de obras e de suas cópias (agregação). */
public class ObraService {

    private final ObraDAO obraDAO = new ObraDAO();
    private final CopiaDAO copiaDAO = new CopiaDAO();

    public void salvar(Obra obra) throws NegocioException, SQLException {
        if (obra.getTitulo() == null || obra.getTitulo().isBlank()) {
            throw new NegocioException("O título é obrigatório.");
        }
        if (obra.getAutor() == null || obra.getAutor().isBlank()) {
            throw new NegocioException("O autor é obrigatório.");
        }
        if (obra.getAno() < 0 || obra.getAno() > java.time.Year.now().getValue()) {
            throw new NegocioException("Ano de publicação inválido.");
        }

        if (obra.getId() == null) {
            obraDAO.salvar(obra);
        } else {
            obraDAO.atualizar(obra);
        }
    }

    /** Cria uma cópia nova para a obra, com o código informado. */
    public Copia adicionarCopia(Obra obra, String codigo) throws NegocioException, SQLException {
        if (codigo == null || codigo.isBlank()) {
            throw new NegocioException("Informe o código da cópia.");
        }
        if (buscarCopiaPorCodigo(codigo.trim()) != null) {
            throw new NegocioException("Já existe uma cópia com o código " + codigo.trim() + ".");
        }

        Copia copia = new Copia(codigo.trim());
        obra.adicionarCopia(copia);   // agregação: liga os dois lados
        copiaDAO.salvar(copia);       // grava com o obra_id
        return copia;
    }

    public void excluir(Obra obra) throws NegocioException, SQLException {
        if (!obra.getCopias().isEmpty()) {
            throw new NegocioException("A obra possui cópias cadastradas e não pode ser excluída.");
        }
        obraDAO.excluir(obra.getId());
    }

    public List<Obra> listarTodas() throws SQLException {
        return obraDAO.listarTodos();
    }

    /**
     * Procura uma cópia pelo código em todas as obras.
     * Usado na tela de empréstimo, onde o funcionário digita o código.
     * Devolve null se não encontrar.
     */
    public Copia buscarCopiaPorCodigo(String codigo) throws SQLException {
        for (Obra obra : obraDAO.listarTodos()) {
            for (Copia copia : obra.getCopias()) {
                if (copia.getCodigo().equalsIgnoreCase(codigo)) {   // ignora maiúsculas/minúsculas
                    return copia;
                }
            }
        }
        return null;
    }
}