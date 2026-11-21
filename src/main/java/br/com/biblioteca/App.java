package br.com.biblioteca;

import java.sql.SQLException;

import br.com.biblioteca.dao.CopiaDAO;
import br.com.biblioteca.dao.InicializadorBanco;
import br.com.biblioteca.dao.ObraDAO;
import br.com.biblioteca.model.Copia;
import br.com.biblioteca.model.Obra;
import br.com.biblioteca.model.SituacaoCopia;

public class App {
    public static void main(String[] args) {
        try {
            InicializadorBanco.criarTabelas();

            ObraDAO obraDAO = new ObraDAO();
            CopiaDAO copiaDAO = new CopiaDAO();

            // Só cadastra se ainda não houver obras
            if (obraDAO.listarTodos().isEmpty()) {
                Obra obra = new Obra("Dom Casmurro", "Machado de Assis", "Garnier", 1899, "978-85-0000-000-0");
                obraDAO.salvar(obra);              // 1º salva a obra, para ela ganhar um id

                Copia c1 = new Copia("C001");
                Copia c2 = new Copia("C002");
                obra.adicionarCopia(c1);           // 2º liga as cópias à obra
                obra.adicionarCopia(c2);
                copiaDAO.salvar(c1);               // 3º salva as cópias (usam o id da obra)
                copiaDAO.salvar(c2);
                System.out.println("Obra e cópias cadastradas.");
            }

            // Lê tudo do banco: as obras voltam com as cópias dentro
            for (Obra o : obraDAO.listarTodos()) {
                System.out.println(o + " - " + o.getCopias().size() + " cópias, disponível? "
                    + o.temCopiaDisponivel());
                for (Copia c : o.getCopias()) {
                    System.out.println("   " + c + " [" + c.getSituacao() + "]");
                }
            }

            // Busca uma cópia pelo id, alterna a situação e salva
            Copia copia = copiaDAO.buscarPorId(1);
            if (copia.isDisponivel()) {
                copia.setSituacao(SituacaoCopia.EMPRESTADA);
            } else {
                copia.setSituacao(SituacaoCopia.DISPONIVEL);
            }
            copiaDAO.atualizar(copia);
            System.out.println("Cópia " + copia + " agora está " + copia.getSituacao());

        } catch (SQLException e) {
            System.out.println("Erro no banco de dados: " + e.getMessage());
        }
    }
}