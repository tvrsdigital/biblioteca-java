package br.com.biblioteca.model;

public class Professor extends Leitor {

    //constantes
    private static final int PRAZO_DIAS = 14; 
    private static final int LIMITE_EMPRESTIMOS = 5;

    //construtores
    public Professor(String nome, String cpf, String email, String telefone, String matricula) {
        super(nome, cpf, email, telefone, matricula);
    }
    public Professor(Integer id, String nome, String cpf, String email, String telefone, String matricula) {
        super(id, nome, cpf, email, telefone, matricula);
    }     
    
    //metodos
    @Override
    public int getPrazoDias() {
    return PRAZO_DIAS;
    }

    public int getPrazoDias(){
        return PRAZO_DIAS;
    }

    public int getLimiteEmprestimos(){
        return LIMITE_EMPRESTIMOS;
    }

    public String getTipo(){
        return "PROFESSOR"; 
    }
}
