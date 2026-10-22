package com.example.gearrent.infra.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.email.remetente}")
    private String remetente;

    @Value("${app.email.envio-habilitado}")
    private boolean envioHabilitado;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void enviarRecuperacaoSenha(String destinatario, String nome, String link, long validadeMinutos) {
        if (!envioHabilitado) {
            log.warn("[DESENVOLVIMENTO] Envio de e-mail desabilitado. Link de recuperação para {}: {}",
                    destinatario, link);
            return;
        }

        String texto = "Olá, " + nome + "!\n\n"
                + "Recebemos um pedido para criar uma nova senha no GearRent.\n\n"
                + "Para criar a nova senha, clique no link abaixo:\n"
                + link + "\n\n"
                + "Este link vale por " + validadeMinutos + " minutos e só pode ser usado uma vez.\n\n"
                + "Se você não fez este pedido, pode ignorar esta mensagem. A sua senha continua a mesma.\n\n"
                + "Equipe GearRent";

        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(remetente);
            mensagem.setTo(destinatario);
            mensagem.setSubject("GearRent: crie uma nova senha");
            mensagem.setText(texto);
            mailSender.send(mensagem);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de recuperação de senha", e);
        }
    }
}