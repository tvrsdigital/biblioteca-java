package br.com.biblioteca.model;

public abstract class Pessoa {
    // atributos
    private Integer id;
    private String nome;
    private String cpf;
    private String email;
    private String telefone;

    // construtores

    //construtor para objetos novos, sem id. 
    protected Pessoa(String nome, String cpf, String email, String telefone) {
        this(null,nome,cpf,email,telefone); 
    }

    //construtor para objetos lidos do banco (já possuem id) 
    protected Pessoa(Integer id, String nome, String cpf, String email, String telefone) {
        this.id = id; 
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.telefone = telefone;
     }

    // getters e setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    // sobrescrita de método herdado
    @Override
    public String toString() {
        return nome;
    }

}