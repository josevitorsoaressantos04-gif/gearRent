package com.example.gearrent.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Random;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class Usuario {
    private long id;
    private Boolean ativo;
    private String nome;
    private String cpf;
    private String dataNascimento;
    private String email;
    private String senha;
    private String telefone;
}
