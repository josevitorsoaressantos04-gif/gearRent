package com.example.gearrent.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "usuario_avatar")
@Data
@NoArgsConstructor
public class UsuarioAvatar {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false)
    private long tamanho;

    // bytea no PostgreSQL (sem @Lob de propósito)
    @Column(nullable = false)
    private byte[] conteudo;
}