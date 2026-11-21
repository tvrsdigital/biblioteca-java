package br.com.biblioteca.model;

public class Aluno extends Leitor {

    //constantes
    private static final int PRAZO_DIAS = 7;
    private static final int LIMITE_EMPRESTIMOS = 3;

    //construtores 
    public Aluno(String nome, String cpf, String email, String telefone, String matricula) {
        super(nome, cpf, email, telefone, matricula);
    }
    public Aluno(Integer id, String nome, String cpf, String email, String telefone, String matricula) {
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
        return "ALUNO"; 
    }
    
}
