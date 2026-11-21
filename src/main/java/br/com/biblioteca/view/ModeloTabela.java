package br.com.biblioteca.view;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Modelo de tabela genérico para qualquer lista de objetos.
 *
 * GENERICS: T é o tipo dos objetos (Leitor, Obra, Copia...).
 * TEMPLATE METHOD: esta classe cuida da lista, das linhas e das colunas;
 * cada subclasse só implementa valorDaColuna(), dizendo o que mostrar.
 */
public abstract class ModeloTabela<T> extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private final String[] colunas;
    private List<T> itens = new ArrayList<>();

    // String... = varargs: recebe quantos nomes de coluna forem passados
    protected ModeloTabela(String... colunas) {
        this.colunas = colunas;
    }

    /** O que mostrar na coluna informada, para um item. Cada subclasse define. */
    protected abstract Object valorDaColuna(T item, int coluna);

    public void setItens(List<T> itens) {
        this.itens = new ArrayList<>(itens);
        fireTableDataChanged();
    }

    public T getItem(int linha) {
        return itens.get(linha);
    }

    public List<T> getItens() {
        return itens;
    }

    @Override
    public int getRowCount() {
        return itens.size();
    }

    @Override
    public int getColumnCount() {
        return colunas.length;
    }

    @Override
    public String getColumnName(int coluna) {
        return colunas[coluna];
    }

    @Override
    public Object getValueAt(int linha, int coluna) {
        return valorDaColuna(itens.get(linha), coluna);
    }
}