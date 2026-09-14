package br.insper.cursos.repository;

import br.insper.cursos.entity.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findAllByDeletadoFalseOrderByNomeAsc();

    List<Curso> findAllByDeletadoFalseAndNomeStartingWithIgnoreCaseOrderByNomeAsc(String nome);

    Optional<Curso> findByIdAndDeletadoFalse(Long id);
}
