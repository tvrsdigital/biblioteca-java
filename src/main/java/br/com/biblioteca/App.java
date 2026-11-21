package br.com.biblioteca;

import java.time.LocalDate;
import java.util.List;

import br.com.biblioteca.model.Aluno;
import br.com.biblioteca.model.Copia;
import br.com.biblioteca.model.Emprestimo;
import br.com.biblioteca.model.Funcionario;
import br.com.biblioteca.model.Leitor;
import br.com.biblioteca.model.Obra;
import br.com.biblioteca.model.Professor;
import br.com.biblioteca.model.SituacaoCopia;


public class App {
    public static void main(String[] args) {
        Aluno ana = new Aluno("Ana", "111", "ana@email.com", "1199999", "A001");
        Professor carlos = new Professor("Carlos", "222", "carlos@email.com", "1188888", "P001");
        Funcionario func = new Funcionario("Bia", "333", "bia@email.com", "1177777", "Atendente", "bia", "hash", "salt");

        Obra obra = new Obra("Dom Casmurro", "Machado de Assis", "Garnier", 1899, "978-85-0000-000-0");
        Copia c1 = new Copia("C001");
        Copia c2 = new Copia("C002");
        obra.adicionarCopia(c1);
        obra.adicionarCopia(c2);

        LocalDate hoje = LocalDate.of(2026, 11, 21);
        Emprestimo e1 = new Emprestimo(ana, c1, func, hoje);
        Emprestimo e2 = new Emprestimo(carlos, c2, func, hoje);
        System.out.println("Ana devolve até: " + e1.getDataPrevista());    // 2026-11-28
        System.out.println("Carlos devolve até: " + e2.getDataPrevista()); // 2026-12-05

        LocalDate dezDiasDepois = hoje.plusDays(10);
        System.out.println("Ana atrasada? " + e1.estaAtrasado(dezDiasDepois));    // true
        System.out.println("Carlos atrasado? " + e2.estaAtrasado(dezDiasDepois)); // false

        e1.registrarDevolucao(dezDiasDepois);
        System.out.println("Multa da Ana: R$ " + e1.getMulta()); // 3 dias de atraso → 3.00
    }

}
