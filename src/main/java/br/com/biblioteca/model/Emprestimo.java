package br.com.biblioteca.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Emprestimo {
    
    //constante
    public static final BigDecimal MULTA_POR_DIA = new BigDecimal("1.00"); 
    
    //atributos
    private Integer id; 
    private Leitor leitor; 
    private Copia copia; 
    private Funcionario funcionario;
    private LocalDate dataEmprestimo;  
    private LocalDate dataPrevista;  
    private LocalDate dataDevolucao; 
    private BigDecimal multa;
   
    //construtor novo
    public Emprestimo(Leitor leitor, Copia copia, Funcionario funcionario, LocalDate dataEmprestimo) {
        this.leitor = leitor;
        this.copia = copia;
        this.funcionario = funcionario;
        this.dataEmprestimo = dataEmprestimo;
        this.dataPrevista = dataEmprestimo.plusDays(leitor.getPrazoDias());
        this.dataDevolucao = null;
        this.multa = BigDecimal.ZERO;
    }

    //construtor banco 
    public Emprestimo(Integer id, Leitor leitor, Copia copia, Funcionario funcionario, LocalDate dataPrevista,
            LocalDate dataEmprestimo, LocalDate dataDevolucao, BigDecimal multa) {
        this.id = id;
        this.leitor = leitor;
        this.copia = copia;
        this.funcionario = funcionario;
        this.dataPrevista = dataPrevista;
        this.dataEmprestimo = dataEmprestimo;
        this.dataDevolucao = dataDevolucao;
        this.multa = multa;
    }

    //getters e setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Leitor getLeitor() {
        return leitor;
    }

    public Copia getCopia() {
        return copia;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public LocalDate getDataPrevista() {
        return dataPrevista;
    }

    public LocalDate getDataEmprestimo() {
        return dataEmprestimo;
    }

    public LocalDate getDataDevolucao() {
        return dataDevolucao;
    }

    public BigDecimal getMulta() {
        return multa;
    }

    //metodos
    public boolean isDevolvido() {
        return dataDevolucao != null;
    }

    public boolean estaAtrasado(LocalDate hoje) {
        return !isDevolvido() && hoje.isAfter(dataPrevista);
    }

    public BigDecimal calcularMulta(LocalDate data) {
        long diasAtraso = ChronoUnit.DAYS.between(dataPrevista, data);
        if (diasAtraso <= 0) {
            return BigDecimal.ZERO;
        }
        return MULTA_POR_DIA.multiply(BigDecimal.valueOf(diasAtraso));
    }
    
    public void registrarDevolucao(LocalDate data) {
        this.dataDevolucao = data;
        this.multa = calcularMulta(data);
    }
}
