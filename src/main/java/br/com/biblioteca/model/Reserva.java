package br.com.biblioteca.model;

import java.time.LocalDate;

public class Reserva {
    private Integer id; 
    private Leitor leitor; 
    private Obra obra; 
    private LocalDate dataReserva; 
    private SituacaoReserva situacao;
   
    //contrutor novo
    public Reserva(Leitor leitor, Obra obra, LocalDate dataReserva) {
        this(null, leitor, obra, dataReserva, SituacaoReserva.ATIVA);
    }

    //construtor do banco
    public Reserva(Integer id, Leitor leitor, Obra obra, LocalDate dataReserva, SituacaoReserva situacao) {
        this.id = id;
        this.leitor = leitor;
        this.obra = obra;
        this.dataReserva = dataReserva;
        this.situacao = situacao;
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

    public Obra getObra() {
        return obra;
    }

    public LocalDate getDataReserva() {
        return dataReserva;
    }

    public SituacaoReserva getSituacao() {
        return situacao;
    } 

     //metodos

    public boolean isAtiva(){
        return situacao == SituacaoReserva.ATIVA;  
     }

    public void atender(){
        if (!isAtiva()){
            throw new IllegalStateException("Só é possível atender uma reserva ativa.");
        }
        this.situacao = SituacaoReserva.ATENDIDA; 
    }

    public void cancelar(){
        if(!isAtiva()){
            throw new IllegalStateException("Só é possível cancelar uma reserva ativa.");
        }
        this.situacao = SituacaoReserva.CANCELADA; 
    }
     
}
