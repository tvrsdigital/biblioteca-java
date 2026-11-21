package br.com.biblioteca;

import java.util.List;

import br.com.biblioteca.model.Aluno;
import br.com.biblioteca.model.Leitor;
import br.com.biblioteca.model.Professor;


public class App {
    public static void main(String[] args) {
        List<Leitor> leitores = List.of(
            new Aluno("Ana", "111", "ana@email.com", "1199999", "A001"),
            new Professor("Carlos", "222", "carlos@email.com", "1188888", "P001")
        );

        for (Leitor l : leitores) {
            System.out.println(l + " (" + l.getTipo() + "): prazo "
                + l.getPrazoDias() + " dias, limite " + l.getLimiteEmprestimos());
        }
    }
}
