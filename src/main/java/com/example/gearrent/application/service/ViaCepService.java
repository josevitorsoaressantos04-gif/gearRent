package com.example.gearrent.application.service;

import com.example.gearrent.application.DTO.EnderecoDTO;
import com.example.gearrent.application.DTO.ViaCepResponseDTO;
import com.example.gearrent.domain.exception.RecursoNaoEncontradoException;
import com.example.gearrent.domain.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Serviço responsável por consultar a API externa do ViaCEP e converter a resposta
 * diretamente para o EnderecoDTO utilizado nos cadastros de Clientes e da Locadora.
 */
@Service
public class ViaCepService {

    private final RestClient restClient;

    public ViaCepService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://viacep.com.br/ws")
                .build();
    }

    /**
     * Limpa a máscara do CEP, valida se possui 8 dígitos numéricos,
     * consulta o ViaCEP e devolve o EnderecoDTO preenchido com os dados disponíveis.
     */
    public EnderecoDTO consultarPorCep(String cepInformado) {
        String cepLimpo = cepInformado == null ? "" : cepInformado.replaceAll("\\D", "");
        if (cepLimpo.length() != 8) {
            throw new RegraDeNegocioException("Por favor, informe um CEP válido com 8 números.");
        }

        try {
            ViaCepResponseDTO resposta = restClient.get()
                    .uri("/{cep}/json/", cepLimpo)
                    .retrieve()
                    .body(ViaCepResponseDTO.class);

            if (resposta == null || resposta.possuiErro() || resposta.cep() == null) {
                throw new RecursoNaoEncontradoException("CEP não encontrado. Confira os números informados.");
            }

            return EnderecoDTO.deViaCep(resposta);
        } catch (RecursoNaoEncontradoException | RegraDeNegocioException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new RegraDeNegocioException("Não foi possível consultar o CEP no ViaCEP no momento. Preencha o endereço manualmente ou tente novamente.");
        }
    }
}
