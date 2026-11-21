package br.com.biblioteca.model;

public abstract class Leitor extends Pessoa {
    private String matricula;

    //construtores
    protected Leitor(String nome, String cpf, String email, String telefone, String matricula) {
        super(nome, cpf, email, telefone);
        this.matricula = matricula;
    }

    protected Leitor(Integer id, String nome, String cpf, String email, String telefone, String matricula) {
        super(id, nome, cpf, email, telefone);
        this.matricula = matricula;
    }

    //getters e setters 
    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    } 
    
    //metodos

    public abstract int getPrazoDias();
    public abstract int getLimiteEmprestimos();
    public abstract String getTipo();

}
