package com.example.gearrent.controllers;

import com.example.gearrent.DTO.AtualizarStatusRequest;
import com.example.gearrent.DTO.AtualizarStatusResponse;
import com.example.gearrent.DTO.UsuarioRequest;
import com.example.gearrent.DTO.UsuarioResponse;
import com.example.gearrent.entities.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        // Retorno mockado: futuramente será substituído por Page<Usuario>
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criarUsuario(@RequestBody UsuarioRequest usuarioRequest) {
        // TODO: A senha do usuário deve ser "hashada" (ex: BCrypt) antes de salvar no banco
        Usuario usuarioBanco = new Usuario();
            usuarioBanco.setId(1L);
            usuarioBanco.setAtivo(true);
            usuarioBanco.setNome(usuarioRequest.nome());
            usuarioBanco.setEmail(usuarioRequest.email());
            usuarioBanco.setSenha(usuarioRequest.senha());
            usuarioBanco.setCpf(usuarioRequest.cpf());
            usuarioBanco.setDataCadastro(LocalDateTime.now());
            return ResponseEntity.ok(new UsuarioResponse(usuarioBanco.getId(), "Usuario atualizado com sucesso"));


    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizarUsuario(@PathVariable Long id, @RequestBody UsuarioRequest usuarioRequest) {

        //validar se existe no banco
        Usuario usuarioBanco = new Usuario();
        if(usuarioBanco != null){
            usuarioBanco.setNome(usuarioRequest.nome());
            usuarioBanco.setEmail(usuarioRequest.email());
            usuarioBanco.setSenha(usuarioRequest.senha());
            usuarioBanco.setCpf(usuarioRequest.cpf());
            usuarioBanco.setDataAtualizacao(LocalDateTime.now());
            return ResponseEntity.ok(new UsuarioResponse(usuarioBanco.getId(), "Usuario atualizado com sucesso"));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UsuarioResponse> excluirUsuario(@PathVariable Long id) {
        Usuario usuarioBanco = new Usuario();
        if (usuarioBanco != null) {
            usuarioBanco.setAtivo(false);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }


    @PatchMapping("{id}/status")
    public ResponseEntity<AtualizarStatusResponse> atualizarStatusUsuario(@RequestBody AtualizarStatusRequest atualizarStatusRequest,
                                                                          @PathVariable Long id) {
        Usuario usuarioBanco = new Usuario();
        if(usuarioBanco != null){
            usuarioBanco.setAtivo(usuarioBanco.getAtivo());
            return ResponseEntity.ok(new AtualizarStatusResponse(id, "Usuario atualizado com sucesso"));
        }
        return ResponseEntity.notFound().build();
    }

}