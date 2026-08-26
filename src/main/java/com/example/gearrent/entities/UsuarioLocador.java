package com.example.gearrent.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class UsuarioLocador extends Usuario{
    public UsuarioLocador(String nome, String cpf, String dataNascimento, String email, String telefone, String senha, String cnpj, String razaoSocial) {
        super(nome, cpf, dataNascimento, email, telefone, senha);
    }
}
