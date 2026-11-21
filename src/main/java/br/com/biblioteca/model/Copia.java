package br.com.biblioteca.model;

public class Copia {
    private Integer id; 
    private String codigo; 
    private SituacaoCopia situacao; 
    private Obra obra;

    //construtores

    //para copia nova
    public Copia(String codigo) {
        this(null, codigo, SituacaoCopia.DISPONIVEL);
    }

    //construtor do banco de dados 
    public Copia(Integer id, String codigo, SituacaoCopia situacao) {
        this.id = id;
        this.codigo = codigo;
        this.situacao = situacao;
    }

    //getters e setters 
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getCodigo() {
        return codigo;
    }
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
    public SituacaoCopia getSituacao() {
        return situacao;
    }
    public void setSituacao(SituacaoCopia situacao) {
        this.situacao = situacao;
    }
    public Obra getObra() {
        return obra;
    }
    public void setObra(Obra obra) {
        this.obra = obra;
    } 

    //metodos
    public boolean isDisponivel(){
        return situacao == SituacaoCopia.DISPONIVEL; 
    }

    @Override 
    public String toString(){
        if (obra == null){
            return codigo;
        }
        return codigo + " - " + obra.getTitulo();
    }
}
