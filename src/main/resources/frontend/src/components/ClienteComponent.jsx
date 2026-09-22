import React, { useState, useEffect } from 'react';
import api from '../services/api.js'; // Importa a instância do Axios

export function ClienteComponent() {
    const [clientes, setClientes] = useState([]);
    const [nome, setNome] = useState('');
    const [cpf, setCpf] = useState('');
    const [email, setEmail] = useState('');

    // Busca lista de clientes (GET /clientes)
    const carregarClientes = async () => {
        try {
            const response = await api.get('/clientes');
            setClientes(response.data); // Axios entrega os dados parseados em .data
        } catch (err) {
            alert(err.message);
        }
    };

    useEffect(() => {
        carregarClientes();
    }, []);

    // Envia DTO do cliente (POST /clientes)
    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            // Envia o payload diretamente como objeto JavaScript
            await api.post('/clientes', { nome, cpf, email });
            alert('Cliente cadastrado com sucesso!');

            setNome('');
            setCpf('');
            setEmail('');
            carregarClientes();
        } catch (err) {
            alert(`Falha no cadastro: ${err.message}`);
        }
    };

    return (
        <div className="container">
            <h2>Cadastro de Clientes</h2>
            <form onSubmit={handleSubmit}>
                <input value={nome} onChange={(e) => setNome(e.target.value)} placeholder="Nome" required />
                <input value={cpf} onChange={(e) => setCpf(e.target.value)} placeholder="CPF" required />
                <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="E-mail" required />
                <button type="submit">Cadastrar</button>
            </form>

            <h3>Lista de Clientes Cadastrados</h3>
            <ul>
                {clientes.map((c) => (
                    <li key={c.id}>{c.nome} - {c.email}</li>
                ))}
            </ul>
        </div>
    );
}