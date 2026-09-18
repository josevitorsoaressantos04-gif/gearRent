import React, { useState } from 'react';
import api from '../services/api';

export function CadastroUsuario() {
    const [formData, setFormData] = useState({
        nome: '',
        cpf: '',
        dataNascimento: '',
        email: '',
        senha: '',
        telefone: ''
    });

    const [mensagem, setMensagem] = useState(null);
    const [erro, setErro] = useState(null);

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMensagem(null);
        setErro(null);

        try {
            // Dispara POST /usuarios repassando o UsuarioRequest
            const response = await api.post('/usuarios', formData);
            setMensagem(response.data.mensagem || 'Usuário cadastrado com sucesso!');

            // Limpa os campos do formulário
            setFormData({
                nome: '',
                cpf: '',
                dataNascimento: '',
                email: '',
                senha: '',
                telefone: ''
            });
        } catch (err) {
            setErro(err.message || 'Erro ao cadastrar usuário.');
        }
    };

    return (
        <div style={{ maxWidth: '500px', margin: '40px auto', padding: '20px', border: '1px solid #ccc', borderRadius: '8px' }}>
            <h2>Cadastro de Usuário - GearRent</h2>

            {mensagem && <div style={{ color: 'green', marginBottom: '10px' }}>{mensagem}</div>}
            {erro && <div style={{ color: 'red', marginBottom: '10px' }}>{erro}</div>}

            <form onSubmit={handleSubmit}>
                <div style={{ marginBottom: '10px' }}>
                    <label>Nome Completo:</label>
                    <input type="text" name="nome" value={formData.nome} onChange={handleChange} required style={{ width: '100%', padding: '8px' }} />
                </div>

                <div style={{ marginBottom: '10px' }}>
                    <label>CPF:</label>
                    <input type="text" name="cpf" value={formData.cpf} onChange={handleChange} placeholder="000.000.000-00" required style={{ width: '100%', padding: '8px' }} />
                </div>

                <div style={{ marginBottom: '10px' }}>
                    <label>Data de Nascimento:</label>
                    <input type="date" name="dataNascimento" value={formData.dataNascimento} onChange={handleChange} required style={{ width: '100%', padding: '8px' }} />
                </div>

                <div style={{ marginBottom: '10px' }}>
                    <label>E-mail Corporativo:</label>
                    <input type="email" name="email" value={formData.email} onChange={handleChange} required style={{ width: '100%', padding: '8px' }} />
                </div>

                <div style={{ marginBottom: '10px' }}>
                    <label>Senha:</label>
                    <input type="password" name="senha" value={formData.senha} onChange={handleChange} required style={{ width: '100%', padding: '8px' }} />
                </div>

                <div style={{ marginBottom: '15px' }}>
                    <label>Telefone / WhatsApp:</label>
                    <input type="text" name="telefone" value={formData.telefone} onChange={handleChange} placeholder="(11) 99999-9999" required style={{ width: '100%', padding: '8px' }} />
                </div>

                <button type="submit" style={{ width: '100%', padding: '10px', backgroundColor: '#EAB308', border: 'none', fontWeight: 'bold', cursor: 'pointer' }}>
                    Cadastrar Usuário
                </button>
            </form>
        </div>
    );
}