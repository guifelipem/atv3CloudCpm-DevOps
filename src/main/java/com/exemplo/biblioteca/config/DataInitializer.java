package com.exemplo.biblioteca.config;

import com.exemplo.biblioteca.model.Autor;
import com.exemplo.biblioteca.model.Livro;
import com.exemplo.biblioteca.repository.AutorRepository;
import com.exemplo.biblioteca.repository.LivroRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner carregarDados(AutorRepository autores, LivroRepository livros) {
        return args -> {
            Autor machado = autores.save(new Autor("Machado de Assis", "Brasileira"));
            Autor clarice = autores.save(new Autor("Clarice Lispector", "Brasileira"));
            livros.save(new Livro("Dom Casmurro", "978000000001", machado));
            livros.save(new Livro("A Hora da Estrela", "978000000002", clarice));
        };
    }
}
