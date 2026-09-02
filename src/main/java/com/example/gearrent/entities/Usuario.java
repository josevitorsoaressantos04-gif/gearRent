package com.example.gearrent.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Random;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {
    private long id;
    private Boolean ativo;
    private String nome;
    private String cpf;
    private String dataNascimento;
    private String email;
    private String senha;
    private String telefone;
    private LocalDateTime dataCadastro;
    private LocalDateTime dataAtualizacao;
}
