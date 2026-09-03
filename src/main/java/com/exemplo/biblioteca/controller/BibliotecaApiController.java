package com.exemplo.biblioteca.controller;

import com.exemplo.biblioteca.model.Autor;
import com.exemplo.biblioteca.model.Livro;
import com.exemplo.biblioteca.repository.AutorRepository;
import com.exemplo.biblioteca.repository.LivroRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BibliotecaApiController {
    private final AutorRepository autorRepository;
    private final LivroRepository livroRepository;

    public BibliotecaApiController(AutorRepository autorRepository, LivroRepository livroRepository) {
        this.autorRepository = autorRepository;
        this.livroRepository = livroRepository;
    }

    @GetMapping("/autores")
    public List<Autor> autores() {
        return autorRepository.findAll();
    }

    @GetMapping("/livros")
    public List<Livro> livros() {
        return livroRepository.findAll();
    }
}
