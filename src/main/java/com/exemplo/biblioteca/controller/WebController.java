package com.exemplo.biblioteca.controller;

import com.exemplo.biblioteca.model.Autor;
import com.exemplo.biblioteca.model.Livro;
import com.exemplo.biblioteca.repository.AutorRepository;
import com.exemplo.biblioteca.repository.LivroRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class WebController {
    private final AutorRepository autorRepository;
    private final LivroRepository livroRepository;

    public WebController(AutorRepository autorRepository, LivroRepository livroRepository) {
        this.autorRepository = autorRepository;
        this.livroRepository = livroRepository;
    }

    @GetMapping("/login")
    String login() { return "login"; }

    @GetMapping("/")
    String home(Model model, Authentication auth) {
        model.addAttribute("usuario", auth.getName());
        model.addAttribute("autores", autorRepository.count());
        model.addAttribute("livros", livroRepository.count());
        return "index";
    }

    @GetMapping("/autores")
    String autores(Model model) {
        model.addAttribute("autores", autorRepository.findAll());
        model.addAttribute("autor", new Autor());
        return "autores";
    }

    @PostMapping("/autores")
    String salvarAutor(@ModelAttribute Autor autor) {
        autorRepository.save(autor);
        return "redirect:/autores";
    }

    @GetMapping("/autores/excluir/{id}")
    String excluirAutor(@PathVariable Long id) {
        if (livroRepository.findAll().stream().noneMatch(l -> l.getAutor().getId().equals(id))) {
            autorRepository.deleteById(id);
        }
        return "redirect:/autores";
    }

    @GetMapping("/livros")
    String livros(Model model) {
        model.addAttribute("livros", livroRepository.findAll());
        model.addAttribute("autores", autorRepository.findAll());
        model.addAttribute("livro", new Livro());
        return "livros";
    }

    @PostMapping("/livros")
    String salvarLivro(@RequestParam String titulo, @RequestParam String isbn, @RequestParam Long autorId) {
        Autor autor = autorRepository.findById(autorId).orElseThrow();
        livroRepository.save(new Livro(titulo, isbn, autor));
        return "redirect:/livros";
    }

    @GetMapping("/livros/excluir/{id}")
    String excluirLivro(@PathVariable Long id) {
        livroRepository.deleteById(id);
        return "redirect:/livros";
    }

    @GetMapping("/admin/usuarios")
    String usuarios() { return "usuarios"; }
}
