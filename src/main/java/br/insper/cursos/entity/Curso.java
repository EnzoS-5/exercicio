package br.insper.cursos.entity;

import br.insper.cursos.dto.CursoRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cursos")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(nullable = false)
    private Integer cargaHoraria;

    @JsonIgnore
    @Column(nullable = false)
    private boolean deletado;

    protected Curso() {
    }

    public Curso(String nome, String descricao, Integer cargaHoraria) {
        this.nome = nome;
        this.descricao = descricao;
        this.cargaHoraria = cargaHoraria;
        this.deletado = false;
    }

    public static Curso fromRequest(CursoRequest request) {
        return new Curso(request.nome(), request.descricao(), request.cargaHoraria());
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public Integer getCargaHoraria() {
        return cargaHoraria;
    }

    public boolean isDeletado() {
        return deletado;
    }

    public void deletar() {
        this.deletado = true;
    }
}
