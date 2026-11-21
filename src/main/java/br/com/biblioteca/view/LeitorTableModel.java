package br.com.biblioteca.view;

import br.com.biblioteca.model.Leitor;

/** Modelo da tabela de leitores: só define o conteúdo das colunas. */
public class LeitorTableModel extends ModeloTabela<Leitor> {

    private static final long serialVersionUID = 1L;

    public LeitorTableModel() {
        super("Nome", "Tipo", "Matrícula", "CPF", "Prazo", "Limite");
    }

    @Override
    protected Object valorDaColuna(Leitor l, int coluna) {
        return switch (coluna) {
            case 0 -> l.getNome();
            case 1 -> l.getTipo();
            case 2 -> l.getMatricula();
            case 3 -> l.getCpf();
            case 4 -> l.getPrazoDias() + " dias";        // POLIMORFISMO na tela
            case 5 -> l.getLimiteEmprestimos() + " livros";
            default -> "";
        };
    }
}