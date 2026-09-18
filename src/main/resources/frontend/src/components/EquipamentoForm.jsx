import React, { useState } from 'react';
import { equipamentoService } from '../services/equipamentoService';

export function EquipamentoForm({ onSucesso }) {
    const [formData, setFormData] = useState({
        numeroPatrimonio: '',
        nome: '',
        modelo: '',
        valorDiariaBase: ''
    });

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await equipamentoService.cadastrar({
                ...formData,
                valorDiariaBase: parseFloat(formData.valorDiariaBase)
            });
            alert('Equipamento cadastrado com sucesso!');
            setFormData({ numeroPatrimonio: '', nome: '', modelo: '', valorDiariaBase: '' });
            if (onSucesso) onSucesso();
        } catch (error) {
            alert('Erro ao cadastrar equipamento.');
        }
    };

    return (
        <form onSubmit={handleSubmit} style={{ marginBottom: '20px' }}>
            <h3>Cadastrar Novo Equipamento</h3>
            <input name="numeroPatrimonio" placeholder="Patrimônio (ex: EQ-1005)" value={formData.numeroPatrimonio} onChange={handleChange} required />
            <input name="nome" placeholder="Nome do Equipamento" value={formData.nome} onChange={handleChange} required />
            <input name="modelo" placeholder="Modelo" value={formData.modelo} onChange={handleChange} required />
            <input name="valorDiariaBase" type="number" step="0.01" placeholder="Diária (R$)" value={formData.valorDiariaBase} onChange={handleChange} required />
            <button type="submit">Salvar no Estoque</button>
        </form>
    );
}