package br.com.biblioteca;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;

import br.com.biblioteca.dao.CopiaDAO;
import br.com.biblioteca.dao.EmprestimoDAO;
import br.com.biblioteca.dao.FuncionarioDAO;
import br.com.biblioteca.dao.InicializadorBanco;
import br.com.biblioteca.dao.LeitorDAO;
import br.com.biblioteca.dao.ObraDAO;
import br.com.biblioteca.dao.ReservaDAO;
import br.com.biblioteca.model.Aluno;
import br.com.biblioteca.model.Copia;
import br.com.biblioteca.model.Emprestimo;
import br.com.biblioteca.model.Funcionario;
import br.com.biblioteca.model.Leitor;
import br.com.biblioteca.model.Obra;
import br.com.biblioteca.model.Professor;
import br.com.biblioteca.model.Reserva;
import br.com.biblioteca.model.SituacaoCopia;
import br.com.biblioteca.service.FuncionarioService;

/**
 * Recria o banco com dados prontos para a apresentação.
 * As datas são relativas a HOJE, então funciona em qualquer dia:
 * sempre haverá um empréstimo atrasado, um ativo, um devolvido com multa e uma reserva.
 *
 * Usa os DAOs diretamente (e não os services) porque precisa criar
 * empréstimos com datas no passado, o que as regras normais não permitiriam.
 */
public class PreparacaoApresentacao {

    public static void main(String[] args) throws Exception {
        Files.deleteIfExists(Path.of("biblioteca.db"));
        InicializadorBanco.criarTabelas();
        new FuncionarioService().garantirAdministrador();
        Funcionario admin = new FuncionarioDAO().buscarPorLogin("admin");
        LocalDate hoje = LocalDate.now();

        // ---- Leitores: dois alunos e dois professores ----
        LeitorDAO leitorDAO = new LeitorDAO();
        Aluno ana = new Aluno("Ana Souza", "111.111.111-11", "ana@email.com", "(11) 99999-1111", "A001");
        Aluno bruno = new Aluno("Bruno Lima", "222.222.222-22", "bruno@email.com", "(11) 99999-2222", "A002");
        Professor carla = new Professor("Carla Mendes", "333.333.333-33", "carla@email.com", "(11) 99999-3333", "P001");
        Professor diego = new Professor("Diego Rocha", "444.444.444-44", "diego@email.com", "(11) 99999-4444", "P002");
        for (Leitor l : new Leitor[] {ana, bruno, carla, diego}) {
            leitorDAO.salvar(l);
        }

        // ---- Obras e cópias ----
        ObraDAO obraDAO = new ObraDAO();
        CopiaDAO copiaDAO = new CopiaDAO();
        Obra domCasmurro = criarObra(obraDAO, copiaDAO, "Dom Casmurro", "Machado de Assis", "Garnier", 1899, "C001", "C002");
        criarObra(obraDAO, copiaDAO, "O Cortiço", "Aluísio Azevedo", "Garnier", 1890, "C003", "C004", "C005");
        Obra neuromancer = criarObra(obraDAO, copiaDAO, "Neuromancer", "William Gibson", "Ace Books", 1984, "C006");
        criarObra(obraDAO, copiaDAO, "Clean Code", "Robert C. Martin", "Prentice Hall", 2008, "C007", "C008");

        // ---- Empréstimos ----
        EmprestimoDAO emprestimoDAO = new EmprestimoDAO();

        // Ana (aluna): pegou há 12 dias, prazo de 7 -> 5 dias de ATRASO
        emprestar(emprestimoDAO, copiaDAO, ana, domCasmurro.getCopias().get(0), admin, hoje.minusDays(12));

        // Carla (professora): pegou há 3 dias, prazo de 14 -> ATIVO
        emprestar(emprestimoDAO, copiaDAO, carla, domCasmurro.getCopias().get(1), admin, hoje.minusDays(3));

        // Bruno (aluno): pegou há 20 dias e devolveu há 10 -> DEVOLVIDO com 3 dias de multa
        Emprestimo antigo = new Emprestimo(bruno, neuromancer.getCopias().get(0), admin, hoje.minusDays(20));
        emprestimoDAO.salvar(antigo);
        antigo.registrarDevolucao(hoje.minusDays(10));
        emprestimoDAO.atualizarDevolucao(antigo);

        // ---- Reserva: Diego aguarda o Dom Casmurro (todas as cópias emprestadas) ----
        new ReservaDAO().salvar(new Reserva(diego, domCasmurro, hoje.minusDays(1)));

        System.out.println("Banco preparado para a apresentação.");
        System.out.println("Login: admin / admin123");
    }

    private static Obra criarObra(ObraDAO obraDAO, CopiaDAO copiaDAO, String titulo, String autor,
                                  String editora, int ano, String... codigos) throws SQLException {
        Obra obra = new Obra(titulo, autor, editora, ano, null);
        obraDAO.salvar(obra);
        for (String codigo : codigos) {
            Copia copia = new Copia(codigo);
            obra.adicionarCopia(copia);
            copiaDAO.salvar(copia);
        }
        return obra;
    }

    private static void emprestar(EmprestimoDAO emprestimoDAO, CopiaDAO copiaDAO, Leitor leitor,
                                  Copia copia, Funcionario funcionario, LocalDate data) throws SQLException {
        emprestimoDAO.salvar(new Emprestimo(leitor, copia, funcionario, data));
        copia.setSituacao(SituacaoCopia.EMPRESTADA);
        copiaDAO.atualizar(copia);
    }
}