package br.com.biblioteca;

import java.util.List;

import br.com.biblioteca.model.Aluno;
import br.com.biblioteca.model.Copia;
import br.com.biblioteca.model.Leitor;
import br.com.biblioteca.model.Obra;
import br.com.biblioteca.model.Professor;
import br.com.biblioteca.model.SituacaoCopia;


public class App {
    public static void main(String[] args) {
        Obra obra = new Obra("Dom Casmurro", "Machado de Assis", "Garnier", 1899, "978-85-0000-000-0");
        Copia c1 = new Copia("C001");
        Copia c2 = new Copia("C002");
        obra.adicionarCopia(c1);
        obra.adicionarCopia(c2);

        System.out.println(obra + ": " + obra.getCopias().size() + " cópias");
        System.out.println(c1);  // deve mostrar "C001 - Dom Casmurro"

        c1.setSituacao(SituacaoCopia.EMPRESTADA);
        System.out.println("Tem disponível? " + obra.temCopiaDisponivel()); // true (C002)

        c2.setSituacao(SituacaoCopia.EMPRESTADA);
        System.out.println("Tem disponível? " + obra.temCopiaDisponivel()); // false
    }

}
