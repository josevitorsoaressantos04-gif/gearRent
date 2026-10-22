package com.example.gearrent.infra.exception;

import com.example.gearrent.domain.exceptions.*;
import com.example.gearrent.domain.exceptions.CredenciaisInvalidasException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Campos inválidos (@NotBlank, @Email, @CPF...) quando o controller usa @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErroResposta> tratarValidacao(MethodArgumentNotValidException ex) {
        List<String> mensagens = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getDefaultMessage())
                .distinct()
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiErroResposta.de(HttpStatus.BAD_REQUEST, mensagens));
    }

    // JSON malformado, data inválida ou valor de lista (enum) que não existe
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErroResposta> tratarJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiErroResposta.de(HttpStatus.BAD_REQUEST,
                        "Alguma informação foi preenchida de forma incorreta. Confira os campos e tente novamente."));
    }

    // Valor inválido na URL (ex.: id com letras, status que não existe)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErroResposta> tratarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ApiErroResposta.de(HttpStatus.BAD_REQUEST,
                        "Um dos valores informados não é válido. Confira e tente novamente."));
    }

    // Rota que não existe
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErroResposta> tratarRotaInexistente(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErroResposta.de(HttpStatus.NOT_FOUND,
                        "O endereço solicitado não foi encontrado."));
    }

    // Método HTTP errado (ex.: GET em uma rota de POST)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErroResposta> tratarMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiErroResposta.de(HttpStatus.METHOD_NOT_ALLOWED,
                        "Esta operação não é permitida neste endereço."));
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ApiErroResposta> tratarCredenciais(CredenciaisInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErroResposta.de(HttpStatus.UNAUTHORIZED, ex.getMessage()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiErroResposta> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErroResposta.de(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    // Conflitos: documento, e-mail ou patrimônio duplicado, equipamento indisponível
    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ApiErroResposta> tratarConflito(ConflitoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErroResposta.de(HttpStatus.CONFLICT, ex.getMessage()));
    }

    // Rede de segurança para duplicidades que escaparam das validações (ex.: dois cadastros ao mesmo tempo)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErroResposta> tratarIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErroResposta.de(HttpStatus.CONFLICT,
                        "Já existe um registro com estes dados. Confira as informações e tente novamente."));
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ApiErroResposta> tratarRegraDeNegocio(RegraDeNegocioException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErroResposta.de(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()));
    }

    // Último recurso: qualquer erro inesperado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErroResposta> tratarErroGenerico(Exception ex) {
        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErroResposta.de(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Algo deu errado do nosso lado. Tente novamente em instantes."));
    }

    @ExceptionHandler(TokenRecuperacaoInvalidoException.class)
    public ResponseEntity<ApiErroResposta> tratarTokenRecuperacao(TokenRecuperacaoInvalidoException ex) {
        return ResponseEntity.badRequest()
                .body(ApiErroResposta.de(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }
}