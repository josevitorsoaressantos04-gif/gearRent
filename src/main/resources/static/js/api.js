const EQUIPAMENTOS_ENDPOINT = "/api/equipamentos?status=DISPONIVEL";

export async function buscarEquipamentos(endpoint = EQUIPAMENTOS_ENDPOINT) {
    const resposta = await fetch(endpoint, {
        headers: {
            Accept: "application/json"
        }
    });

    if (!resposta.ok) {
        throw new Error(`A API respondeu com o status ${resposta.status}.`);
    }

    const dados = await resposta.json();
    const equipamentos = Array.isArray(dados) ? dados : dados?.content;

    if (!Array.isArray(equipamentos)) {
        throw new Error("A resposta da API não contém uma lista de equipamentos.");
    }

    return equipamentos;
}
