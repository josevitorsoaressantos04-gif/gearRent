import { buscarEquipamentos } from "./api.js";

const listaEquipamentos = document.querySelector("#lista-equipamentos");
const statusCarregamento = document.querySelector("#status-carregamento");
const mensagemErro = document.querySelector("#mensagem-erro");
const botaoTentarNovamente = document.querySelector("#tentar-novamente");

const nomesCategoria = {
    FERRAMENTA_ELETRICA: "Ferramenta elétrica",
    MAQUINARIO_PESADO: "Maquinário pesado",
    ANDAIME_ESCADA: "Andaime ou escada",
    FERRAMENTA_MANUAL: "Ferramenta manual"
};

const formatoMoeda = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL"
});

function criarCartao(equipamento) {
    const cartao = document.createElement("article");
    cartao.className = "equipment-card";

    const categoria = document.createElement("p");
    categoria.className = "equipment-category";
    categoria.textContent = nomesCategoria[equipamento.categoria] || "Equipamento";

    const nome = document.createElement("h3");
    nome.className = "equipment-name";
    nome.textContent = equipamento.nome || "Equipamento sem nome";

    cartao.append(categoria, nome);

    if (equipamento.modelo) {
        const modelo = document.createElement("p");
        modelo.className = "equipment-model";
        modelo.textContent = `Modelo: ${equipamento.modelo}`;
        cartao.append(modelo);
    }

    const preco = document.createElement("p");
    preco.className = "equipment-price";

    const valorDiaria = Number(equipamento.valorDiaria);
    preco.textContent = Number.isFinite(valorDiaria)
        ? `${formatoMoeda.format(valorDiaria)} por dia`
        : "Consulte o valor da diária";
    cartao.append(preco);

    return cartao;
}

function mostrarEquipamentos(equipamentos) {
    listaEquipamentos.replaceChildren();

    if (equipamentos.length === 0) {
        statusCarregamento.textContent = "Nenhum equipamento está disponível no momento.";
        return;
    }

    const cartoes = equipamentos.map(criarCartao);
    listaEquipamentos.append(...cartoes);
    statusCarregamento.textContent = `${equipamentos.length} equipamento(s) disponível(is).`;
}

async function carregarEquipamentos() {
    statusCarregamento.textContent = "Carregando equipamentos...";
    statusCarregamento.hidden = false;
    mensagemErro.hidden = true;
    listaEquipamentos.replaceChildren();
    listaEquipamentos.setAttribute("aria-busy", "true");

    try {
        const equipamentos = await buscarEquipamentos();
        mostrarEquipamentos(equipamentos);
    } catch (erro) {
        console.error("Falha ao buscar equipamentos:", erro);
        statusCarregamento.hidden = true;
        mensagemErro.hidden = false;
    } finally {
        listaEquipamentos.setAttribute("aria-busy", "false");
    }
}

botaoTentarNovamente.addEventListener("click", carregarEquipamentos);
carregarEquipamentos();
