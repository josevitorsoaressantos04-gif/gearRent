import React, { useEffect, useState } from 'react';
import { equipamentoService } from '../services/equipamentoService';

export function EquipamentoList() {
    const [equipamentos, setEquipamentos] = useState([]);
    const [loading, setLoading] = useState(true);

    const carregarFrota = async () => {
        try {
            setLoading(true);
            const data = await equipamentoService.listarTodos();
            setEquipamentos(data);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        carregarFrota();
    }, []);

    const handleStatusChange = async (id, status) => {
        try {
            await equipamentoService.alterarStatus(id, status);
            carregarFrota();
        } catch (err) {
            alert('Falha ao alterar o status do equipamento.');
        }
    };

    if (loading) return <div>Carregando dados da frota...</div>;

    return (
        <div>
            <h2>Frota de Equipamentos - GearRent</h2>
            <table>
                <thead>
                <tr>
                    <th>Patrimônio</th>
                    <th>Nome / Modelo</th>
                    <th>Diária Base</th>
                    <th>Status</th>
                    <th>Ações</th>
                </tr>
                </thead>
                <tbody>
                {equipamentos.map((item) => (
                    <tr key={item.id}>
                        <td>{item.numeroPatrimonio}</td>
                        <td>{item.nome} ({item.modelo})</td>
                        <td>R$ {Number(item.valorDiariaBase).toFixed(2)}</td>
                        <td>{item.status}</td>
                        <td>
                            {item.status === 'AGUARDANDO_INSPECAO' && (
                                <button onClick={() => handleStatusChange(item.id, 'DISPONIVEL')}>Aprovar Inspeção</button>
                            )}
                            {item.status === 'DISPONIVEL' && (
                                <button onClick={() => handleStatusChange(item.id, 'EM_USO')}>Alocar</button>
                            )}
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
}