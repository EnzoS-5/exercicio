package br.insper.cursos.service;

import br.insper.cursos.dto.CursoRequest;
import br.insper.cursos.entity.Curso;
import br.insper.cursos.exception.CursoNaoEncontradoException;
import br.insper.cursos.repository.CursoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CursoService {

    private final CursoRepository cursoRepository;

    public CursoService(CursoRepository cursoRepository) {
        this.cursoRepository = cursoRepository;
    }

    @Transactional(readOnly = true)
    public List<Curso> listar(String nome) {
        if (nome == null || nome.isBlank()) {
            return cursoRepository.findAllByDeletadoFalseOrderByNomeAsc();
        }
        return cursoRepository
                .findAllByDeletadoFalseAndNomeStartingWithIgnoreCaseOrderByNomeAsc(nome.trim());
    }

    @Transactional
    public Curso criar(CursoRequest request) {
        return cursoRepository.save(Curso.fromRequest(request));
    }

    @Transactional
    public void deletar(Long id) {
        Curso curso = cursoRepository.findByIdAndDeletadoFalse(id)
                .orElseThrow(() -> new CursoNaoEncontradoException(id));
        curso.deletar();
        cursoRepository.save(curso);
    }
}
