package br.com.biblioteca.view;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import br.com.biblioteca.model.Leitor;

/**
 * Modelo da tabela de leitores: adapta uma List<Leitor> ao formato que a JTable entende.
 * A JTable não conhece Leitor; ela só pergunta ao modelo quantas linhas e colunas
 * existem e o que vai em cada célula. Respondemos sobrescrevendo os métodos abaixo.
 */
public class LeitorTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUNAS = {"Nome", "Tipo", "Matrícula", "CPF", "Prazo", "Limite"};
    private List<Leitor> leitores = new ArrayList<>();

    public void setLeitores(List<Leitor> leitores) {
        this.leitores = new ArrayList<>(leitores);
        fireTableDataChanged();   // avisa a tabela para se redesenhar
    }

    public Leitor getLeitor(int linha) {
        return leitores.get(linha);
    }

    @Override
    public int getRowCount() {
        return leitores.size();
    }

    @Override
    public int getColumnCount() {
        return COLUNAS.length;
    }

    @Override
    public String getColumnName(int coluna) {
        return COLUNAS[coluna];
    }

    @Override
    public Object getValueAt(int linha, int coluna) {
        Leitor l = leitores.get(linha);
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