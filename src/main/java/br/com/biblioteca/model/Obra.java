package br.com.biblioteca.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Obra {
    private Integer id; 
    private String titulo; 
    private String autor; 
    private String editora; 
    private int ano; 
    private String isbn; 
    private final List<Copia> copias = new ArrayList<>();
    
    //construtores 
    public Obra(String titulo, String autor, String editora, int ano, String isbn) {
        this(null, titulo, autor, editora, ano, isbn);
    }

    public Obra(Integer id, String titulo, String autor, String editora, int ano, String isbn) {
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.editora = editora;
        this.ano = ano;
        this.isbn = isbn;
    }

    //getters e setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getEditora() {
        return editora;
    }

    public void setEditora(String editora) {
        this.editora = editora;
    }

    public int getAno() {
        return ano;
    }

    public void setAno(int ano) {
        this.ano = ano;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    } 

    //metodos
    @Override
    public String toString() {
    return titulo;
    }
    
    public List<Copia> getCopias() {
       return Collections.unmodifiableList(copias);
    }

    public void adicionarCopia(Copia copia) {
        copias.add(copia);   // lado da Obra: a cópia entra na lista
        copia.setObra(this); // lado da Copia: ela passa a conhecer sua obra
    }

    public boolean temCopiaDisponivel() {
        for (Copia copia : copias) {
            if (copia.isDisponivel()) {
                return true;  // achou uma: não precisa olhar o resto
            }
        }
        return false;         // percorreu tudo e não achou
    }
}
