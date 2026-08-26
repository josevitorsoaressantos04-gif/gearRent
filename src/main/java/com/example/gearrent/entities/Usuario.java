package com.example.gearrent.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Random;
import lombok.NoArgsConstructor;

@Data
public class Usuario {
    private long id;
    private String nome;
    private String cpf;
    private String dataNascimento;
    private String email;
    private String senha;
    private String telefone;

    public Usuario(String nome, String cpf, String dataNascimento, String email, String telefone, String senha) {
        this.id = (new Random()).nextLong();
        this.nome = nome;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.email = email;
        this.telefone = telefone;
        this.senha = senha;
    }
}
