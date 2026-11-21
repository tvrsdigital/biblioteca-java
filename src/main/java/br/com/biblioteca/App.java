package br.com.biblioteca;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import br.com.biblioteca.dao.CopiaDAO;
import br.com.biblioteca.dao.EmprestimoDAO;
import br.com.biblioteca.dao.FuncionarioDAO;
import br.com.biblioteca.dao.InicializadorBanco;
import br.com.biblioteca.dao.LeitorDAO;
import br.com.biblioteca.dao.ObraDAO;
import br.com.biblioteca.dao.ReservaDAO;
import br.com.biblioteca.exception.NegocioException;
import br.com.biblioteca.model.Aluno;
import br.com.biblioteca.model.Copia;
import br.com.biblioteca.model.Emprestimo;
import br.com.biblioteca.model.Funcionario;
import br.com.biblioteca.model.Obra;
import br.com.biblioteca.model.Professor;
import br.com.biblioteca.model.Reserva;
import br.com.biblioteca.model.SituacaoCopia;
import br.com.biblioteca.service.AutenticacaoService;
import br.com.biblioteca.service.EmprestimoService;
import br.com.biblioteca.service.LeitorService;
import br.com.biblioteca.service.ObraService;
import br.com.biblioteca.service.ReservaService;
import br.com.biblioteca.util.SenhaUtil;

public class App {

    // Data fixa para o teste dar sempre o mesmo resultado
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 21);

    public static void main(String[] args) throws Exception {
        // ===== CENÁRIO DE TESTE: recria o banco do zero =====
        Files.deleteIfExists(Path.of("biblioteca.db"));
        InicializadorBanco.criarTabelas();

        String salt = SenhaUtil.gerarSalt();
        new FuncionarioDAO().salvar(new Funcionario("Administrador", "000.000.000-00",
            "admin@biblioteca.com", "0000000", "Bibliotecário", "admin",
            SenhaUtil.gerarHash("admin123", salt), salt));

        Aluno ana = new Aluno("Ana", "111.111.111-11", "ana@email.com", "1199999", "A001");
        Professor carlos = new Professor("Carlos", "222.222.222-22", "carlos@email.com", "1188888", "P001");
        new LeitorDAO().salvar(ana);
        new LeitorDAO().salvar(carlos);

        Obra obra = new Obra("Dom Casmurro", "Machado de Assis", "Garnier", 1899, "978-85-0000-000-0");
        new ObraDAO().salvar(obra);
        Copia c1 = new Copia("C001");
        Copia c2 = new Copia("C002");
        obra.adicionarCopia(c1);
        obra.adicionarCopia(c2);
        CopiaDAO copiaDAO = new CopiaDAO();
        copiaDAO.salvar(c1);
        copiaDAO.salvar(c2);

        // Empréstimo antigo e atrasado da Ana (gravado direto, só para montar o cenário)
        Funcionario adminCenario = new FuncionarioDAO().buscarPorLogin("admin");
        new EmprestimoDAO().salvar(new Emprestimo(ana, c2, adminCenario, LocalDate.of(2026, 11, 1)));
        c2.setSituacao(SituacaoCopia.EMPRESTADA);
        copiaDAO.atualizar(c2);

        // Reserva do Carlos para o Dom Casmurro
        new ReservaDAO().salvar(new Reserva(carlos, obra, HOJE.minusDays(2)));
        System.out.println("=== Cenário montado ===\n");

        // ===== TESTES DAS REGRAS =====
        AutenticacaoService auth = new AutenticacaoService();
        EmprestimoService service = new EmprestimoService();

        tentar("1. Login com senha errada", () -> auth.autenticar("admin", "errada"));
        Funcionario logado = auth.autenticar("admin", "admin123");
        System.out.println("2. Login correto: bem-vindo, " + logado + "\n");

        tentar("3. Ana pega a C001 (tem atraso)",
            () -> service.realizarEmprestimo(ana, copiaDAO.buscarPorId(c1.getId()), logado, HOJE));

        tentar("4. Carlos pega a C001",
            () -> {
                Emprestimo e = service.realizarEmprestimo(carlos, copiaDAO.buscarPorId(c1.getId()), logado, HOJE);
                System.out.println("   OK! Devolver até " + e.getDataPrevista());
            });

        tentar("5. Carlos pega a C002 (está emprestada)",
            () -> service.realizarEmprestimo(carlos, copiaDAO.buscarPorId(c2.getId()), logado, HOJE));

        tentar("6. Ana devolve a C002",
            () -> {
                Emprestimo e = service.registrarDevolucao(copiaDAO.buscarPorId(c2.getId()), HOJE);
                System.out.println("   OK! Multa: R$ " + e.getMulta()
                    + " | C002 agora: " + copiaDAO.buscarPorId(c2.getId()).getSituacao());
            });

        tentar("7. Ana pega a C002 (reservada para o Carlos)",
            () -> service.realizarEmprestimo(ana, copiaDAO.buscarPorId(c2.getId()), logado, HOJE));

        tentar("8. Carlos pega a C002 (ele é o primeiro da fila)",
            () -> {
                service.realizarEmprestimo(carlos, copiaDAO.buscarPorId(c2.getId()), logado, HOJE);
                System.out.println("   OK! Situação da reserva: "
                    + new ReservaDAO().listarTodas().get(0).getSituacao());
            });

                    LeitorService leitorService = new LeitorService();
        ObraService obraService = new ObraService();
        ReservaService reservaService = new ReservaService();

        tentar("9. Cadastrar leitor com matrícula repetida",
            () -> leitorService.salvar(new Aluno("Bruno", "333.333.333-33", "b@email.com", "", "A001")));

        tentar("10. Cadastrar cópia com código repetido",
            () -> obraService.adicionarCopia(obraService.listarTodas().get(0), "c001"));

        tentar("11. Ana reserva o Dom Casmurro (todas as cópias emprestadas)",
            () -> {
                reservaService.reservar(ana, obraService.listarTodas().get(0), HOJE);
                System.out.println("   OK! Ana entrou na fila");
            });

        tentar("12. Ana reserva de novo",
            () -> reservaService.reservar(ana, obraService.listarTodas().get(0), HOJE));

        tentar("13. Excluir a Ana (tem histórico)",
            () -> leitorService.excluir(ana));
    }

    // ----- Auxiliares do teste -----

    // Interface funcional: representa "um bloco de código que pode lançar exceções"
    interface Acao {
        void executar() throws Exception;
    }

    // Executa uma ação e mostra a mensagem se uma regra for violada.
    // É exatamente o que as telas vão fazer, com JOptionPane no lugar do println.
    private static void tentar(String descricao, Acao acao) {
        System.out.println(descricao);
        try {
            acao.executar();
        } catch (NegocioException e) {
            System.out.println("   REGRA: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("   ERRO TÉCNICO: " + e.getMessage());
        }
        System.out.println();
    }
}