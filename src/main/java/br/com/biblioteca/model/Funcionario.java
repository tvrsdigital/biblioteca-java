package br.com.biblioteca.model;

public class Funcionario extends Pessoa {
    private String cargo; 
    private String login; 
    private String senhaHash; 
    private String salt;

    //construtores 
    public Funcionario(String nome, String cpf, String email, String telefone, String cargo, String login,
        String senhaHash, String salt) {
        super(nome, cpf, email, telefone);
        this.cargo = cargo;
        this.login = login;
        this.senhaHash = senhaHash;
        this.salt = salt;
    }
    public Funcionario(Integer id, String nome, String cpf, String email, String telefone, String cargo, String login,
        String senhaHash, String salt) {
        super(id, nome, cpf, email, telefone);
        this.cargo = cargo;
        this.login = login;
        this.senhaHash = senhaHash;
        this.salt = salt;
    }

    //getters e setters 
    public String getCargo() {
        return cargo;
    }
    public void setCargo(String cargo) {
        this.cargo = cargo;
    }
    public String getLogin() {
        return login;
    }
    public void setLogin(String login) {
        this.login = login;
    }
    public String getSenhaHash() {
        return senhaHash;
    }
    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }
    public String getSalt() {
        return salt;
    }
    public void setSalt(String salt) {
        this.salt = salt;
    } 

}
