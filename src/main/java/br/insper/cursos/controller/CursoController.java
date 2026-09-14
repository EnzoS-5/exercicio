package br.insper.cursos.controller;

import br.insper.cursos.dto.CursoRequest;
import br.insper.cursos.entity.Curso;
import br.insper.cursos.service.CursoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/cursos")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    public List<Curso> listar(@RequestParam(required = false) String nome) {
        return cursoService.listar(nome);
    }

    @PostMapping
    public ResponseEntity<Curso> criar(@Valid @RequestBody CursoRequest request) {
        Curso curso = cursoService.criar(request);
        return ResponseEntity.created(URI.create("/cursos/" + curso.getId())).body(curso);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        cursoService.deletar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
