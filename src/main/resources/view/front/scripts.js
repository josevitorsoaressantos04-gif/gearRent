// Simulação de Dados de Estoque (Refletindo a Entidade Equipamento e a Máquina de Estados do Spring Boot)
const mockEquipmentList = [
    { id: 1, patrimonio: "EQ-1001", nome: "Escavadeira Hidráulica 20T", modelo: "Caterpillar 320", horimetro: 1240, diaria: 1200.00, status: "EM_USO" },
    { id: 2, patrimonio: "EQ-1002", nome: "Betoneira 400L", modelo: "CSM 400", horimetro: 320, diaria: 150.00, status: "AGUARDANDO_INSPECAO" },
    { id: 3, patrimonio: "EQ-1003", nome: "Compactador de Solo (Sapo)", modelo: "Mikasa MT-74", horimetro: 85, diaria: 180.00, status: "DISPONIVEL" },
    { id: 4, patrimonio: "EQ-1004", nome: "Gerador de Energia 55 kVA", modelo: "Stemac Silent", horimetro: 2100, diaria: 450.00, status: "EM_MANUTENCAO" },
    { id: 5, patrimonio: "EQ-1005", nome: "Minicarregadeira (Bobcat)", modelo: "Bobcat S530", horimetro: 650, diaria: 550.00, status: "RESERVADO" }
];

const statusLabels = {
    DISPONIVEL: "Disponível",
    RESERVADO: "Reservado",
    EM_USO: "Em Uso",
    AGUARDANDO_INSPECAO: "Aguardando Inspeção",
    EM_MANUTENCAO: "Em Manutenção"
};

// Renderizar Tabela de Equipamentos
function renderEquipmentTable(filter = "ALL") {
    const tableBody = document.getElementById("equipment-table-body");
    tableBody.innerHTML = "";

    const filteredData = mockEquipmentList.filter(item => {
        if (filter === "ALL") return true;
        return item.status === filter;
    });

    filteredData.forEach(item => {
        const row = document.createElement("tr");

        row.innerHTML = `
            <td><strong>${item.patrimonio}</strong></td>
            <td>
                <div><strong>${item.nome}</strong></div>
                <div style="font-size:12px; color:#64748B;">${item.modelo}</div>
            </td>
            <td>${item.horimetro} hrs</td>
            <td>R$ ${item.diaria.toFixed(2)}</td>
            <td>
                <span class="badge badge-${item.status.toLowerCase()}">
                    ${statusLabels[item.status]}
                </span>
            </td>
            <td>
                ${item.status === 'AGUARDANDO_INSPECAO' ? `<button class="btn btn-secondary" style="padding:4px 8px; font-size:12px;" onclick="iniciarInspecao(${item.id})">Inspecionar</button>` : ''}
                ${item.status === 'DISPONIVEL' ? `<button class="btn btn-primary" style="padding:4px 8px; font-size:12px;" onclick="alocarEquipamento(${item.id})">Alocar</button>` : ''}
            </td>
        `;

        tableBody.appendChild(row);
    });
}

// Transições da Máquina de Estados (Aguardando Inspeção -> Disponível)
function iniciarInspecao(id) {
    const item = mockEquipmentList.find(e => e.id === id);
    if (item) {
        item.status = "DISPONIVEL";
        alert(`Inspeção concluída com sucesso para o patrimônio ${item.patrimonio}. Equipamento liberado para novas locações!`);
        renderEquipmentTable(document.getElementById("status-filter").value);
        updateKPIs();
    }
}

// Transição (Disponível -> Em Uso)
function alocarEquipamento(id) {
    const item = mockEquipmentList.find(e => e.id === id);
    if (item) {
        item.status = "EM_USO";
        alert(`Equipamento ${item.patrimonio} alocado em novo contrato com sucesso!`);
        renderEquipmentTable(document.getElementById("status-filter").value);
        updateKPIs();
    }
}

// Atualizador de Indicadores (KPIs)
function updateKPIs() {
    const pendingInspection = mockEquipmentList.filter(e => e.status === "AGUARDANDO_INSPECAO").length;
    document.getElementById("kpi-pending-inspection").innerText = pendingInspection;
}

// Inicializadores
document.addEventListener("DOMContentLoaded", () => {
    renderEquipmentTable();

    // Filtro de Status
    document.getElementById("status-filter").addEventListener("change", (e) => {
        renderEquipmentTable(e.target.value);
    });

    // Eventos do Modal
    const modal = document.getElementById("modal-equipment");
    const btnOpen = document.getElementById("btn-new-equipment");
    const btnClose = document.getElementById("close-equipment-modal");
    const btnCancel = document.getElementById("cancel-equipment-modal");

    btnOpen.addEventListener("click", () => modal.classList.add("active"));
    btnClose.addEventListener("click", () => modal.classList.remove("active"));
    btnCancel.addEventListener("click", () => modal.classList.remove("active"));

    // Submissão do Formulário
    document.getElementById("form-equipment").addEventListener("submit", (e) => {
        e.preventDefault();
        
        const patrimonio = document.getElementById("eq-patrimonio").value;
        const nome = document.getElementById("eq-nome").value;
        const modelo = document.getElementById("eq-modelo").value;
        const diaria = parseFloat(document.getElementById("eq-diaria").value);

        const newId = mockEquipmentList.length + 1;

        mockEquipmentList.push({
            id: newId,
            patrimonio,
            nome,
            modelo,
            horimetro: 0,
            diaria,
            status: "DISPONIVEL"
        });

        alert("Equipamento cadastrado e liberado no estoque com sucesso!");
        modal.classList.remove("active");
        e.target.reset();
        renderEquipmentTable();
    });
});