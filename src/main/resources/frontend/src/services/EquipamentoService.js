import api from './api';

export const equipamentoService = {
  // GET /equipamentos - Lista a frota
  listarTodos: async () => {
    const response = await api.get('/equipamentos');
    return response.data;
  },

  // POST /equipamentos - Cadastra novo equipamento
  cadastrar: async (equipamentoData) => {
    const response = await api.post('/equipamentos', equipamentoData);
    return response.data;
  },

  // PATCH /equipamentos/{id}/status - Transição da Máquina de Estados
  alterarStatus: async (id, novoStatus) => {
    const response = await api.patch(`/equipamentos/${id}/status`, { status: novoStatus });
    return response.data;
  }
};