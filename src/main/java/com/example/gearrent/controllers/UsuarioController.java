package com.example.gearrent.controllers;

import com.example.gearrent.DTO.UsuarioRequest;
import com.example.gearrent.DTO.UsuarioResponse;
import com.example.gearrent.entities.Usuario;
import com.example.gearrent.repository.UsuarioRepository;
import com.example.gearrent.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
@CrossOrigin(origins = {"http://localhost:63342"})
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        return ResponseEntity.ok(usuarioRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> listarUsuariosPorId(@PathVariable Long id) {
        // Se achar, retorna 200 OK com o usuário direto. Se não achar, retorna 404 Not Found.
        return usuarioRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criarUsuario(@RequestBody UsuarioRequest usuarioRequest) {
        // TODO: Mover essa lógica de conversão/hash de senha para dentro do UsuarioService
        Usuario usuarioBanco = new Usuario();
        usuarioBanco.setAtivo(true);
        usuarioBanco.setNome(usuarioRequest.nome());
        usuarioBanco.setEmail(usuarioRequest.email());
        usuarioBanco.setSenha(usuarioRequest.senha());
        usuarioBanco.setCpf(usuarioRequest.cpf());
        usuarioBanco.setDataCadastro(LocalDateTime.now());

        usuarioRepository.save(usuarioBanco);

        // Boa prática: Para criação (POST), o ideal é retornar o status 201 Created
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UsuarioResponse(usuarioBanco.getId(), "Usuário criado com sucesso"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizarUsuario(@PathVariable Long id, @RequestBody UsuarioRequest usuarioRequest) {
        // Faz o findById apenas uma vez
        return usuarioRepository.findById(id)
                .map(usuario -> {
                    usuario.setNome(usuarioRequest.nome());
                    usuario.setEmail(usuarioRequest.email());
                    usuario.setSenha(usuarioRequest.senha());
                    usuario.setCpf(usuarioRequest.cpf());
                    // Geralmente não se altera a data de cadastro no Put, mas mantive conforme seu código
                    usuario.setDataCadastro(LocalDateTime.now());

                    usuarioRepository.save(usuario);
                    return ResponseEntity.ok(new UsuarioResponse(usuario.getId(), "Usuário atualizado com sucesso"));
                })
                .orElse(ResponseEntity.notFound().build()); // Se não encontrar o ID, já manda o 404
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UsuarioResponse> excluirUsuario(@PathVariable Long id) {
        // Faz a checagem uma única vez de forma elegante
        return usuarioRepository.findById(id)
                .map(usuario -> {
                    usuarioService.deleteLogico(id);
                    return ResponseEntity.ok(new UsuarioResponse(usuario.getId(), "Usuário deletado com sucesso"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}