package br.insper.cursos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CursoRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 500) String descricao,
        @NotNull @Min(1) Integer cargaHoraria
) {
}
