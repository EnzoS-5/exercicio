package br.insper.cursos.service;

import br.insper.cursos.dto.CursoRequest;
import br.insper.cursos.entity.Curso;
import br.insper.cursos.exception.CursoNaoEncontradoException;
import br.insper.cursos.repository.CursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    @Mock
    private CursoRepository cursoRepository;

    private CursoService cursoService;

    @BeforeEach
    void setUp() {
        cursoService = new CursoService(cursoRepository);
    }

    @Test
    void deveListarSomenteCursosAtivosSemFiltro() {
        when(cursoRepository.findAllByDeletadoFalseOrderByNomeAsc())
                .thenReturn(List.of(new Curso("Java", "Curso de Java", 20)));

        List<Curso> cursos = cursoService.listar(null);

        assertEquals(1, cursos.size());
        assertEquals("Java", cursos.get(0).getNome());
    }

    @Test
    void deveTratarFiltroEmBrancoComoAusente() {
        when(cursoRepository.findAllByDeletadoFalseOrderByNomeAsc()).thenReturn(List.of());

        List<Curso> cursos = cursoService.listar("   ");

        assertTrue(cursos.isEmpty());
    }

    @Test
    void deveListarPorPrefixoIgnorandoEspacosExternos() {
        when(cursoRepository.findAllByDeletadoFalseAndNomeStartingWithIgnoreCaseOrderByNomeAsc("Ja"))
                .thenReturn(List.of(new Curso("Java", "Curso de Java", 20)));

        List<Curso> cursos = cursoService.listar(" Ja ");

        assertEquals("Java", cursos.get(0).getNome());
    }

    @Test
    void deveCriarCurso() {
        CursoRequest request = new CursoRequest("Spring", "Curso de Spring Boot", 16);
        when(cursoRepository.save(any(Curso.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Curso curso = cursoService.criar(request);

        assertEquals("Spring", curso.getNome());
        assertEquals("Curso de Spring Boot", curso.getDescricao());
        assertEquals(16, curso.getCargaHoraria());
        assertFalse(curso.isDeletado());
    }

    @Test
    void deveRealizarExclusaoLogica() {
        Curso curso = new Curso("Docker", "Curso de Docker", 8);
        when(cursoRepository.findByIdAndDeletadoFalse(1L)).thenReturn(Optional.of(curso));

        cursoService.deletar(1L);

        assertTrue(curso.isDeletado());
        verify(cursoRepository).save(curso);
    }

    @Test
    void deveRetornarErroAoExcluirCursoInexistente() {
        when(cursoRepository.findByIdAndDeletadoFalse(99L)).thenReturn(Optional.empty());

        assertThrows(CursoNaoEncontradoException.class, () -> cursoService.deletar(99L));
        verify(cursoRepository, never()).save(any());
    }
}
