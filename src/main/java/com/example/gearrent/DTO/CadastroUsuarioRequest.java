package com.example.gearrent.DTO;



public record CadastroUsuarioRequest(String nome, String cpf, String dataNascimento, String email, String senha, String telefone) {
}
