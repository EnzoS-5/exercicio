package br.insper.cursos.exception;

public class CursoNaoEncontradoException extends RuntimeException {

    public CursoNaoEncontradoException(Long id) {
        super("Curso com id " + id + " nao encontrado");
    }
}
