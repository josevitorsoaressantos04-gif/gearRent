package com.example.gearrent.domain.entities;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

//anotação @Embedded do JPA serve para "embutir" os atributos de uma classe auxiliar
//diretamente como colunas na tabela da entidade principal
//sem criar uma tabela no banco de dados para isso.


/*
Funções e Capacidades do @Embeddable (A Classe Molde)

Ausência de Identidade Própria: A classe anotada com @Embeddable não recebe a anotação @Id.
Ela não possui existência independente no banco de dados e o seu ciclo de vida está estritamente amarrado
à entidade (como Cliente ou Locacao) que a contém.

Definição de Colunas Internas: Você pode utilizar anotações tradicionais da JPA
(como @Column(name = "nome_rua", nullable = false)) dentro da classe embutível.
O Hibernate respeitará essas restrições e nomenclaturas ao criar as colunas físicas na tabela principal.

Aninhamento: Uma classe @Embeddable pode conter outra classe @Embeddable dentro dela.
Se o seu Endereco precisasse, por exemplo, de um objeto embutido Coordenadas (latitude e longitude),
a arquitetura suportaria esse aninhamento perfeitamente.
 */
@Embeddable
public class Endereco {
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;
}
