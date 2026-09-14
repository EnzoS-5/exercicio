package br.insper.cursos.controller;

import br.insper.cursos.entity.Curso;
import br.insper.cursos.repository.CursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
class CursoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CursoRepository cursoRepository;

    @BeforeEach
    void limparBanco() {
        cursoRepository.deleteAll();
    }

    @Test
    void postCursosDeveCriarCurso() throws Exception {
        mockMvc.perform(post("/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Java Basico",
                                  "descricao": "Fundamentos de Java",
                                  "cargaHoraria": 20
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/cursos/[0-9]+")))
                .andExpect(jsonPath("$.nome").value("Java Basico"))
                .andExpect(jsonPath("$.descricao").value("Fundamentos de Java"))
                .andExpect(jsonPath("$.cargaHoraria").value(20))
                .andExpect(jsonPath("$.deletado").doesNotExist());
    }

    @Test
    void getCursosDeveFiltrarPorPrefixoEOmitirExcluidos() throws Exception {
        cursoRepository.save(new Curso("Java Basico", "Java", 20));
        cursoRepository.save(new Curso("Java Avancado", "Java", 24));
        cursoRepository.save(new Curso("Docker", "Containers", 8));
        Curso removido = cursoRepository.save(new Curso("Java Removido", "Antigo", 4));
        removido.deletar();
        cursoRepository.save(removido);

        mockMvc.perform(get("/cursos").param("nome", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Java Avancado"))
                .andExpect(jsonPath("$[1].nome").value("Java Basico"));
    }

    @Test
    void deleteCursosDeveFazerExclusaoLogica() throws Exception {
        Curso salvo = cursoRepository.save(new Curso("PostgreSQL", "Banco de dados", 12));

        mockMvc.perform(delete("/cursos/{id}", salvo.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cursos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        assertTrue(cursoRepository.findById(salvo.getId()).orElseThrow().isDeletado());
    }
}
