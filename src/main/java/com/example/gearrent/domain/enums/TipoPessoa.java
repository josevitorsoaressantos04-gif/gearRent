package com.example.gearrent.domain.enums;

public enum TipoPessoa {
    FISICA,
    JURIDICA;

    /** 11 dígitos = CPF (física); 14 dígitos = CNPJ (jurídica); qualquer outra coisa = null. */
    public static TipoPessoa deduzirDoDocumento(String documentoSomenteNumeros) {
        if (documentoSomenteNumeros == null) {
            return null;
        }
        return switch (documentoSomenteNumeros.length()) {
            case 11 -> FISICA;
            case 14 -> JURIDICA;
            default -> null;
        };
    }

    public int tamanhoDocumento() {
        return this == FISICA ? 11 : 14;
    }
}
