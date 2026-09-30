package com.example.gearrent.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String nomeFantasia;
    private String razaoSocial;
    private String inscricaoEstadual;


    @Column(unique = true)
    private String cnpj;
    private String telefone;
    private String email;
    private Boolean status;

    // o campo que está em string tem que estar na enitite que você está relacionando
    @OneToMany(mappedBy = "empresa")
    private List<Usuario> usuarios;


}
