import axios from 'axios';

// Criação da instância global apontando para o Spring Boot
const api = axios.create({
    baseURL: 'http://localhost:8080',
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 10000, // Timeout de 10s para evitar chamadas presas
});

// Interceptador de Resposta: Trata erros globais da API
api.interceptors.response.use(
    (response) => response,
    (error) => {
        // Trata erros retornados pelo Spring Boot
        if (error.response) {
            const mensagem = error.response.data?.mensagem || 'Erro no processamento da requisição.';
            console.error(`[API Error ${error.response.status}]:`, mensagem);
            return Promise.reject(new Error(mensagem));
        }

        // Trata falhas de rede ou servidor fora do ar
        if (error.request) {
            console.error('[API Error]: Servidor indisponível.');
            return Promise.reject(new Error('Não foi possível se conectar ao servidor do GearRent.'));
        }

        return Promise.reject(error);
    }
);

export default api;